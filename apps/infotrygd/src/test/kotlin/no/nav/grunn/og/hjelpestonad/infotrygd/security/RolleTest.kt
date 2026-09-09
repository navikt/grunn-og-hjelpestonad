package no.nav.grunn.og.hjelpestonad.infotrygd.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RolleTest {
    @Test
    fun `authority skal returnere ROLE_ prefiks med rollenavn`() {
        assertEquals("ROLE_SAKSBEHANDLER", Rolle.SAKSBEHANDLER.authority())
        assertEquals("ROLE_BESLUTTER", Rolle.BESLUTTER.authority())
        assertEquals("ROLE_LES", Rolle.LES.authority())
    }

    @Test
    fun `fraAzureGrupper skal mappe SAKSBEHANDLER Azure gruppe til SAKSBEHANDLER rolle`() {
        val azureGrupper = listOf("7ce9d1d2-d149-4324-832b-8d459762a102")

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(1, roller.size)
        assertTrue(roller.contains(Rolle.SAKSBEHANDLER))
    }

    @Test
    fun `fraAzureGrupper skal mappe BESLUTTER Azure gruppe til BESLUTTER rolle`() {
        val azureGrupper = listOf("84c4a287-abd6-46c1-bf93-dbf90f1a326d")

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(1, roller.size)
        assertTrue(roller.contains(Rolle.BESLUTTER))
    }

    @Test
    fun `fraAzureGrupper skal mappe LES Azure gruppe til LES rolle`() {
        val azureGrupper = listOf("a181921e-2a55-4198-896b-0086cc805278")

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(1, roller.size)
        assertTrue(roller.contains(Rolle.LES))
    }

    @Test
    fun `fraAzureGrupper skal mappe flere Azure grupper til flere roller`() {
        val azureGrupper =
            listOf(
                "7ce9d1d2-d149-4324-832b-8d459762a102", // SAKSBEHANDLER
                "84c4a287-abd6-46c1-bf93-dbf90f1a326d", // BESLUTTER
            )

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(2, roller.size)
        assertTrue(roller.contains(Rolle.SAKSBEHANDLER))
        assertTrue(roller.contains(Rolle.BESLUTTER))
    }

    @Test
    fun `fraAzureGrupper skal mappe alle Azure grupper til alle roller`() {
        val azureGrupper =
            listOf(
                "7ce9d1d2-d149-4324-832b-8d459762a102", // SAKSBEHANDLER
                "84c4a287-abd6-46c1-bf93-dbf90f1a326d", // BESLUTTER
                "a181921e-2a55-4198-896b-0086cc805278", // LES
            )

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(3, roller.size)
        assertTrue(roller.contains(Rolle.SAKSBEHANDLER))
        assertTrue(roller.contains(Rolle.BESLUTTER))
        assertTrue(roller.contains(Rolle.LES))
    }

    @Test
    fun `fraAzureGrupper skal returnere tomt sett for ukjent Azure gruppe`() {
        val azureGrupper = listOf("00000000-0000-0000-0000-000000000000")

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertTrue(roller.isEmpty())
    }

    @Test
    fun `fraAzureGrupper skal returnere tomt sett for tom liste`() {
        val azureGrupper = emptyList<String>()

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertTrue(roller.isEmpty())
    }

    @Test
    fun `fraAzureGrupper skal ignorere ukjente grupper og kun mappe gyldige`() {
        val azureGrupper =
            listOf(
                "00000000-0000-0000-0000-000000000000", // Ukjent
                "7ce9d1d2-d149-4324-832b-8d459762a102", // SAKSBEHANDLER
                "11111111-1111-1111-1111-111111111111", // Ukjent
                "84c4a287-abd6-46c1-bf93-dbf90f1a326d", // BESLUTTER
            )

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(2, roller.size)
        assertTrue(roller.contains(Rolle.SAKSBEHANDLER))
        assertTrue(roller.contains(Rolle.BESLUTTER))
    }

    @Test
    fun `fraAzureGrupper skal returnere unikt sett selv om samme gruppe oppgis flere ganger`() {
        val azureGrupper =
            listOf(
                "7ce9d1d2-d149-4324-832b-8d459762a102", // SAKSBEHANDLER
                "7ce9d1d2-d149-4324-832b-8d459762a102", // SAKSBEHANDLER (duplikat)
                "7ce9d1d2-d149-4324-832b-8d459762a102", // SAKSBEHANDLER (duplikat)
            )

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(1, roller.size)
        assertTrue(roller.contains(Rolle.SAKSBEHANDLER))
    }

    @Test
    fun `skal ha korrekt beskrivelse for hver rolle`() {
        assertEquals("Kan saksbehandle i saksbehandler-løsningen", Rolle.SAKSBEHANDLER.beskrivelse)
        assertEquals("Kan attestere vedtak i saksbehandling-løsningen", Rolle.BESLUTTER.beskrivelse)
        assertEquals("Kan lese og se informasjon i saksbehandling-løsningen", Rolle.LES.beskrivelse)
    }
}
