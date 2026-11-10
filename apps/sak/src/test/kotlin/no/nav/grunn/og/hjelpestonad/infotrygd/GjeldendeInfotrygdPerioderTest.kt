package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdTestdata.delytelse
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdTestdata.evaluering
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdTestdata.stønad
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdTestdata.vedtak
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdStonadResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GjeldendeInfotrygdPerioderTest {
    @Test
    fun `ett løpende vedtak gir én åpen periode`() {
        val perioder = utled(stønad(vedtak(vedtakId = 1, fom = "2020-01-01")))

        assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", null, 1))
    }

    @Test
    fun `ingen vedtak gir ingen perioder`() {
        assertThat(GjeldendeInfotrygdPerioder.utled(emptyList())).isEmpty()
        assertThat(utled(stønad())).isEmpty()
    }

    @Test
    fun `nyere vedtak overskriver eldre der de overlapper, og eldre gjelder igjen etterpå`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2020-01-01"),
                    vedtak(vedtakId = 2, fom = "2022-01-01", tom = "2022-12-31"),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(
            Forenklet("2020-01-01", "2021-12-31", 1),
            Forenklet("2022-01-01", "2022-12-31", 2),
            Forenklet("2023-01-01", null, 1),
        )
    }

    @Test
    fun `nyere vedtak uten tom overskriver alt framover`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2025-12-31"),
                    vedtak(vedtakId = 2, fom = "2022-01-01"),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(
            Forenklet("2020-01-01", "2021-12-31", 1),
            Forenklet("2022-01-01", null, 2),
        )
    }

    @Test
    fun `eldre vedtak overskriver ikke nyere, uansett rekkefølgen i svaret`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 2, fom = "2022-01-01", tom = "2022-12-31"),
                    vedtak(vedtakId = 1, fom = "2020-01-01"),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(
            Forenklet("2020-01-01", "2021-12-31", 1),
            Forenklet("2022-01-01", "2022-12-31", 2),
            Forenklet("2023-01-01", null, 1),
        )
    }

    @Test
    fun `vedtakId bestemmer rekkefølgen, ikke tidspunktReg`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2020-01-01", tidspunktReg = LocalDateTime.parse("2024-01-01T00:00:00")),
                    vedtak(vedtakId = 2, fom = "2020-01-01", tidspunktReg = LocalDateTime.parse("2020-01-01T00:00:00")),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", null, 2))
    }

    @Test
    fun `nyere vedtak som dekker hele det eldre gir én periode`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2021-01-01", tom = "2021-12-31"),
                    vedtak(vedtakId = 2, fom = "2020-01-01", tom = "2022-12-31"),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", "2022-12-31", 2))
    }

    @Test
    fun `går på tvers av stønader, og perioden får stønaden vedtaket hører til`() {
        val perioder =
            utled(
                stønad(vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2023-12-31"), stønadId = 10),
                stønad(vedtak(vedtakId = 2, fom = "2022-01-01"), stønadId = 20),
            )

        assertThat(perioder.map { it.stønadId to it.vedtak.vedtakId }).containsExactly(10L to 1L, 20L to 2L)
        assertThat(perioder.forenklet()).containsExactly(
            Forenklet("2020-01-01", "2021-12-31", 1),
            Forenklet("2022-01-01", null, 2),
        )
    }

    @Nested
    inner class KuttVedOpphør {
        // Delytelsen i siste periode må ha samme periode som vedtaket etter kuttet, se InfotrygdBeløp.
        @Test
        fun `opphør midt i et vedtak kutter tom ved opphørsdatoen`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01", delytelser = listOf(delytelse(fom = "2020-01-01", tom = "2021-06-15"))),
                        datoOpphør = LocalDate.parse("2021-06-15"),
                    ),
                )

            assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", "2021-06-15", 1))
        }

        @Test
        fun `opphør den 1 i måneden gir tom siste dag i måneden før`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01", delytelser = listOf(delytelse(fom = "2020-01-01", tom = "2021-05-31"))),
                        datoOpphør = LocalDate.parse("2021-06-01"),
                    ),
                )

            assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", "2021-05-31", 1))
        }

        @Test
        fun `opphør siste dag i måneden brukes som den er`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01", delytelser = listOf(delytelse(fom = "2020-01-01", tom = "2021-06-30"))),
                        datoOpphør = LocalDate.parse("2021-06-30"),
                    ),
                )

            assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", "2021-06-30", 1))
        }

        @Test
        fun `vedtak som slutter før opphøret beholder tom`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2020-12-31"),
                        datoOpphør = LocalDate.parse("2021-06-30"),
                    ),
                )

            assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", "2020-12-31", 1))
        }

        @Test
        fun `alle vedtakene på stønaden kuttes før de legges på tidslinjen, så eldre vedtak ikke gjelder igjen etter opphøret`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01"),
                        vedtak(
                            vedtakId = 2,
                            fom = "2021-01-01",
                            tom = "2025-12-31",
                            delytelser = listOf(delytelse(fom = "2021-01-01", tom = "2022-06-30")),
                        ),
                        datoOpphør = LocalDate.parse("2022-06-30"),
                    ),
                )

            assertThat(perioder.forenklet()).containsExactly(
                Forenklet("2020-01-01", "2020-12-31", 1),
                Forenklet("2021-01-01", "2022-06-30", 2),
            )
        }

        @Test
        fun `eldre vedtak på en annen stønad gjelder igjen etter at det nyere er kuttet ved opphør`() {
            val perioder =
                utled(
                    stønad(vedtak(vedtakId = 1, fom = "2020-01-01"), stønadId = 10),
                    stønad(vedtak(vedtakId = 2, fom = "2022-01-01"), stønadId = 20, datoOpphør = LocalDate.parse("2022-12-31")),
                )

            assertThat(perioder.forenklet()).containsExactly(
                Forenklet("2020-01-01", "2021-12-31", 1),
                Forenklet("2022-01-01", "2022-12-31", 2),
                Forenklet("2023-01-01", null, 1),
            )
        }

        @Test
        fun `vedtak som starter samme dag som opphøret er opphørt fra start og tas ikke med`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2021-03-14"),
                        vedtak(vedtakId = 2, fom = "2021-03-15"),
                        datoOpphør = LocalDate.parse("2021-03-15"),
                    ),
                )

            assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", "2021-03-14", 1))
        }

        @Test
        fun `vedtak som starter på en opphørsdato den 1 er opphørt fra start og tas ikke med`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2021-06-30"),
                        vedtak(vedtakId = 2, fom = "2021-07-01"),
                        datoOpphør = LocalDate.parse("2021-07-01"),
                    ),
                )

            assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", "2021-06-30", 1))
        }

        @Test
        fun `vedtak som får tom før fom etter kuttet gir feil`() {
            assertThatThrownBy {
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01"),
                        vedtak(vedtakId = 4711, fom = "2022-01-01"),
                        datoOpphør = LocalDate.parse("2021-07-01"),
                    ),
                )
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessage(
                    "Vedtak 4711 starter 01.01.2022, etter at stønaden opphørte 01.07.2021. " +
                        "Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
                )
        }

        @Test
        fun `vedtak som er ugyldig uten kuttet gir samme feil som før`() {
            assertThatThrownBy {
                utled(
                    stønad(
                        vedtak(vedtakId = 4711, fom = "2022-01-01", tom = "2021-12-31"),
                        datoOpphør = LocalDate.parse("2021-07-01"),
                    ),
                )
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessage("Vedtak 4711 i Infotrygd har innvilget tom 31.12.2021, som ikke er etter innvilget fom 01.01.2022.")
        }
    }

    @Nested
    inner class UklareSaker {
        @Test
        fun `løpende vedtak på flere stønader gir feil`() {
            assertThatThrownBy {
                utled(
                    stønad(vedtak(vedtakId = 1, fom = "2020-01-01"), stønadId = 10),
                    stønad(vedtak(vedtakId = 2, fom = "2022-01-01"), stønadId = 20),
                )
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessage(
                    "Personen har løpende vedtak på flere stønader i Infotrygd (10, 20). " +
                        "Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt teamet.",
                )
        }

        @Test
        fun `løpende vedtak på en stønad som er kuttet ved opphør regnes ikke som løpende`() {
            val perioder =
                utled(
                    stønad(vedtak(vedtakId = 1, fom = "2020-01-01"), stønadId = 10, datoOpphør = LocalDate.parse("2020-12-31")),
                    stønad(vedtak(vedtakId = 2, fom = "2022-01-01"), stønadId = 20),
                )

            assertThat(perioder.forenklet()).containsExactly(
                Forenklet("2020-01-01", "2020-12-31", 1),
                Forenklet("2022-01-01", null, 2),
            )
        }

        @Test
        fun `siste vedtak avsluttet etter datogrensen på en stønad som ikke er opphørt gir feil`() {
            assertThatThrownBy {
                utled(stønad(vedtak(vedtakId = 4711, fom = "2025-01-01", tom = "2026-06-30"), stønadId = 10))
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessage(
                    "Stønad 10 er ikke opphørt, men siste vedtak 4711 sluttet 30.06.2026. " +
                        "Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt teamet.",
                )
        }

        @Test
        fun `siste vedtak avsluttet på datogrensen gir feil`() {
            assertThatThrownBy {
                utled(stønad(vedtak(vedtakId = 4711, fom = "2025-01-01", tom = "2026-01-01"), stønadId = 10))
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessageContainingAll("Stønad 10", "4711", "01.01.2026")
        }

        @Test
        fun `stønad som ikke er opphørt og med siste vedtak avsluttet før datogrensen regnes som avsluttet`() {
            val perioder = utled(stønad(vedtak(vedtakId = 1, fom = "2025-01-01", tom = "2025-12-31")))

            assertThat(perioder.forenklet()).containsExactly(Forenklet("2025-01-01", "2025-12-31", 1))
        }

        @Test
        fun `siste vedtak avsluttet etter datogrensen på en opphørt stønad gir ikke feil`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2025-01-01", tom = "2026-06-30"),
                        datoOpphør = LocalDate.parse("2026-07-01"),
                    ),
                )

            assertThat(perioder.forenklet()).containsExactly(Forenklet("2025-01-01", "2026-06-30", 1))
        }

        @Test
        fun `bare personens siste vedtak sjekkes mot datogrensen`() {
            val perioder =
                utled(
                    stønad(vedtak(vedtakId = 1, fom = "2025-01-01", tom = "2026-06-30"), stønadId = 10),
                    stønad(vedtak(vedtakId = 2, fom = "2026-07-01"), stønadId = 20),
                )

            assertThat(perioder.forenklet()).containsExactly(
                Forenklet("2025-01-01", "2026-06-30", 1),
                Forenklet("2026-07-01", null, 2),
            )
        }

        @Test
        fun `feilmeldingene har ikke fnr`() {
            val tilfeller =
                listOf(
                    listOf(stønad(vedtak(vedtakId = 1, fom = "2022-01-01"), datoOpphør = LocalDate.parse("2021-07-01"))),
                    listOf(stønad(vedtak(vedtakId = 1), stønadId = 10), stønad(vedtak(vedtakId = 2), stønadId = 20)),
                    listOf(stønad(vedtak(vedtakId = 1, fom = "2025-01-01", tom = "2026-06-30"))),
                    listOf(stønad(vedtak(vedtakId = 1, delytelser = emptyList()))),
                    listOf(stønad(vedtak(vedtakId = 1, delytelser = listOf(delytelse(), delytelse())))),
                    listOf(stønad(vedtak(vedtakId = 1, fom = "2020-01-01", delytelser = listOf(delytelse(fom = "2020-01-01", tom = "2020-12-31"))))),
                )

            tilfeller.forEach { stønader ->
                assertThatThrownBy { utled(*stønader.toTypedArray()) }
                    .isInstanceOf(UgyldigInput::class.java)
                    .message()
                    .doesNotContain(stønader.first().personnr)
            }
        }
    }

    @Test
    fun `tidsrom uten vedtak gir ingen periode`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2020-12-31"),
                    vedtak(vedtakId = 2, fom = "2022-01-01", tom = "2022-12-31"),
                ),
            )

        assertThat(perioder.forenklet()).containsExactly(
            Forenklet("2020-01-01", "2020-12-31", 1),
            Forenklet("2022-01-01", "2022-12-31", 2),
        )
    }

    @Nested
    inner class UgyldigPeriode {
        @Test
        fun `vedtak med tom før fom gir feil`() {
            assertThatThrownBy {
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01"),
                        vedtak(vedtakId = 4711, fom = "2022-01-01", tom = "2021-12-31"),
                    ),
                )
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessageContainingAll("4711", "01.01.2022", "31.12.2021")
        }

        @Test
        fun `vedtak med tom lik fom gir feil`() {
            assertThatThrownBy {
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01"),
                        vedtak(vedtakId = 4711, fom = "2022-01-01", tom = "2022-01-01"),
                    ),
                )
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessageContainingAll("4711", "01.01.2022")
        }

        @Test
        fun `vedtak med tom dagen etter fom er med`() {
            val perioder = utled(stønad(vedtak(vedtakId = 1, fom = "2022-01-01", tom = "2022-01-02")))

            assertThat(perioder.forenklet()).containsExactly(Forenklet("2022-01-01", "2022-01-02", 1))
        }
    }

    @Test
    fun `perioden har vedtaket med arv`() {
        val perioder =
            utled(
                stønad(
                    vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2020-12-31", evaluering = evaluering("A00")),
                    vedtak(vedtakId = 2, fom = "2021-01-01"),
                ),
            )

        val periode2 = perioder.single { it.vedtak.vedtakId == 2L }
        assertThat(
            periode2.vedtak.evaluering
                ?.verdi
                ?.diagnose1,
        ).isEqualTo("A00")
        assertThat(
            periode2.vedtak.evaluering
                ?.arvetFraVedtak
                ?.vedtakId,
        ).isEqualTo(1)
    }

    @Nested
    inner class InfotrygdBeløp {
        @Test
        fun `hver periode har beløpet fra delytelsen på vedtaket som gjelder`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01", delytelser = listOf(delytelse(fom = "2020-01-01", beløp = "720"))),
                        vedtak(
                            vedtakId = 2,
                            fom = "2022-01-01",
                            tom = "2022-12-31",
                            delytelser = listOf(delytelse(fom = "2022-01-01", tom = "2022-12-31", beløp = "744")),
                        ),
                    ),
                )

            assertThat(
                perioder.map { listOf(it.fom.toString(), it.tom?.toString(), it.vedtak.vedtakId, it.infotrygdBeløp.toPlainString()) },
            ).containsExactly(
                listOf("2020-01-01", "2021-12-31", 1L, "720"),
                listOf("2022-01-01", "2022-12-31", 2L, "744"),
                listOf("2023-01-01", null, 1L, "720"),
            )
        }

        @Test
        fun `tilstøtende vedtak med like beløp beholdes som to perioder`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(
                            vedtakId = 1,
                            fom = "2020-01-01",
                            tom = "2020-12-31",
                            delytelser = listOf(delytelse(fom = "2020-01-01", tom = "2020-12-31", beløp = "720")),
                        ),
                        vedtak(vedtakId = 2, fom = "2021-01-01", delytelser = listOf(delytelse(fom = "2021-01-01", beløp = "720"))),
                    ),
                )

            assertThat(perioder.forenklet()).containsExactly(
                Forenklet("2020-01-01", "2020-12-31", 1),
                Forenklet("2021-01-01", null, 2),
            )
            assertThat(perioder.map { it.infotrygdBeløp.toPlainString() }).containsExactly("720", "720")
        }

        @Test
        fun `delytelse som slutter før vedtaket gir feil, også i en tidligere periode`() {
            assertThatThrownBy {
                utled(
                    stønad(
                        vedtak(
                            vedtakId = 4711,
                            fom = "2020-01-01",
                            tom = "2021-12-31",
                            delytelser = listOf(delytelse(fom = "2020-01-01", tom = "2021-06-30")),
                        ),
                        vedtak(vedtakId = 4712, fom = "2022-01-01"),
                    ),
                )
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessage(
                    "Vedtak 4711 gjelder 01.01.2020–31.12.2021, men delytelsen med beløpet i Infotrygd gjelder " +
                        "01.01.2020–30.06.2021. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
                )
        }

        @Test
        fun `delytelse som starter etter vedtaket gir feil, og løpende perioder skrives med fra`() {
            assertThatThrownBy {
                utled(stønad(vedtak(vedtakId = 4711, fom = "2020-01-01", delytelser = listOf(delytelse(fom = "2020-02-01")))))
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessage(
                    "Vedtak 4711 gjelder fra 01.01.2020, men delytelsen med beløpet i Infotrygd gjelder fra 01.02.2020. " +
                        "Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
                )
        }

        @Test
        fun `delytelse som varer lenger enn vedtaket i en tidligere periode gir beløpet, og vedtaket avgjør perioden`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(
                            vedtakId = 1,
                            fom = "2020-01-01",
                            tom = "2021-12-31",
                            delytelser = listOf(delytelse(fom = "2019-07-01", beløp = "720")),
                        ),
                        vedtak(vedtakId = 2, fom = "2022-01-01", delytelser = listOf(delytelse(fom = "2022-01-01", beløp = "744"))),
                    ),
                )

            assertThat(perioder.forenklet()).containsExactly(
                Forenklet("2020-01-01", "2021-12-31", 1),
                Forenklet("2022-01-01", null, 2),
            )
            assertThat(perioder.map { it.infotrygdBeløp.toPlainString() }).containsExactly("720", "744")
        }

        @Test
        fun `delytelse som varer lenger enn vedtaket i den siste perioden gir feil`() {
            assertThatThrownBy {
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2021-12-31"),
                        vedtak(vedtakId = 4711, fom = "2022-01-01", delytelser = listOf(delytelse(fom = "2022-01-01"))),
                        datoOpphør = LocalDate.parse("2022-06-30"),
                    ),
                )
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessage(
                    "Vedtak 4711 gjelder 01.01.2022–30.06.2022, men delytelsen med beløpet i Infotrygd gjelder fra 01.01.2022. " +
                        "Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
                )
        }

        @Test
        fun `vedtak uten delytelse gir feil`() {
            assertThatThrownBy {
                utled(
                    stønad(
                        vedtak(vedtakId = 4711, fom = "2020-01-01", tom = "2021-12-31", delytelser = emptyList()),
                        vedtak(vedtakId = 4712, fom = "2022-01-01"),
                    ),
                )
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessage(
                    "Vedtak 4711 gjelder 01.01.2020–31.12.2021, men har ingen delytelse med beløpet i Infotrygd. " +
                        "Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
                )
        }

        @Test
        fun `vedtak med flere delytelser gir feil, også når beløpet er likt`() {
            assertThatThrownBy {
                utled(
                    stønad(
                        vedtak(
                            vedtakId = 4711,
                            fom = "2020-01-01",
                            delytelser = listOf(delytelse(fom = "2020-01-01"), delytelse(fom = "2020-01-01")),
                        ),
                    ),
                )
            }.isInstanceOf(UgyldigInput::class.java)
                .hasMessage(
                    "Vedtak 4711 har 2 delytelser i Infotrygd, ikke én. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
                )
        }

        @Test
        fun `vedtak som er helt overskrevet av et nyere, sjekkes ikke`() {
            val perioder =
                utled(
                    stønad(
                        vedtak(vedtakId = 1, fom = "2020-01-01", tom = "2020-12-31", delytelser = emptyList()),
                        vedtak(vedtakId = 2, fom = "2020-01-01"),
                    ),
                )

            assertThat(perioder.forenklet()).containsExactly(Forenklet("2020-01-01", null, 2))
        }
    }

    @Nested
    inner class AntallVedtakUtenforRekkefølge {
        @Test
        fun `er null når tidspunktReg stiger med vedtakId`() {
            val stønader = InfotrygdArv.fyllVedtakMedVerdierFraTidligereVedtak(listOf(stønad(vedtak(vedtakId = 1), vedtak(vedtakId = 2), vedtak(vedtakId = 3))))

            assertThat(GjeldendeInfotrygdPerioder.antallVedtakUtenforRekkefølge(stønader)).isZero()
        }

        @Test
        fun `teller vedtak registrert før vedtaket med nærmeste lavere vedtakId, på tvers av stønader`() {
            val stønader =
                InfotrygdArv.fyllVedtakMedVerdierFraTidligereVedtak(
                    listOf(
                        stønad(vedtak(vedtakId = 1, tidspunktReg = LocalDateTime.parse("2022-01-01T00:00:00")), stønadId = 1),
                        stønad(vedtak(vedtakId = 2, tidspunktReg = LocalDateTime.parse("2021-01-01T00:00:00")), stønadId = 2),
                    ),
                )

            assertThat(GjeldendeInfotrygdPerioder.antallVedtakUtenforRekkefølge(stønader)).isEqualTo(1)
        }
    }

    private data class Forenklet(
        val fom: String,
        val tom: String?,
        val vedtakId: Long,
    )

    private fun List<GjeldendeInfotrygdPeriode>.forenklet() = map { Forenklet(it.fom.toString(), it.tom?.toString(), it.vedtak.vedtakId) }

    private fun utled(vararg stønader: InfotrygdStonadResponse) = GjeldendeInfotrygdPerioder.utled(InfotrygdArv.fyllVedtakMedVerdierFraTidligereVedtak(stønader.toList()))
}
