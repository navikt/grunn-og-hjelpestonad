package no.nav.gjenlevende.bs.infotrygd.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RolleTest {
    @Test
    fun `authority skal returnere ROLE_ prefiks med rollenavn`() {
        assertEquals("ROLE_SAKSBEHANDLER", Rolle.SAKSBEHANDLER.authority())
        assertEquals("ROLE_ATTESTERING", Rolle.ATTESTERING.authority())
        assertEquals("ROLE_LES", Rolle.LES.authority())
    }

    @Test
    fun `fraAzureGrupper skal mappe SAKSBEHANDLER Azure gruppe til SAKSBEHANDLER rolle`() {
        val azureGrupper = listOf("5b6745de-b65d-40eb-a6f5-860c8b61c27f")

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(1, roller.size)
        assertTrue(roller.contains(Rolle.SAKSBEHANDLER))
    }

    @Test
    fun `fraAzureGrupper skal mappe ATTESTERING Azure gruppe til ATTESTERING rolle`() {
        val azureGrupper = listOf("70cfce24-7865-4676-9fdc-b676e90bfc92")

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(1, roller.size)
        assertTrue(roller.contains(Rolle.ATTESTERING))
    }

    @Test
    fun `fraAzureGrupper skal mappe LES Azure gruppe til LES rolle`() {
        val azureGrupper = listOf("609a78e7-e0bd-491c-a63b-96a09ec62b9b")

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(1, roller.size)
        assertTrue(roller.contains(Rolle.LES))
    }

    @Test
    fun `fraAzureGrupper skal mappe flere Azure grupper til flere roller`() {
        val azureGrupper =
            listOf(
                "5b6745de-b65d-40eb-a6f5-860c8b61c27f", // SAKSBEHANDLER
                "70cfce24-7865-4676-9fdc-b676e90bfc92", // ATTESTERING
            )

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(2, roller.size)
        assertTrue(roller.contains(Rolle.SAKSBEHANDLER))
        assertTrue(roller.contains(Rolle.ATTESTERING))
    }

    @Test
    fun `fraAzureGrupper skal mappe alle Azure grupper til alle roller`() {
        val azureGrupper =
            listOf(
                "5b6745de-b65d-40eb-a6f5-860c8b61c27f", // SAKSBEHANDLER
                "70cfce24-7865-4676-9fdc-b676e90bfc92", // ATTESTERING
                "609a78e7-e0bd-491c-a63b-96a09ec62b9b", // LES
            )

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(3, roller.size)
        assertTrue(roller.contains(Rolle.SAKSBEHANDLER))
        assertTrue(roller.contains(Rolle.ATTESTERING))
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
                "5b6745de-b65d-40eb-a6f5-860c8b61c27f", // SAKSBEHANDLER
                "11111111-1111-1111-1111-111111111111", // Ukjent
                "70cfce24-7865-4676-9fdc-b676e90bfc92", // ATTESTERING
            )

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(2, roller.size)
        assertTrue(roller.contains(Rolle.SAKSBEHANDLER))
        assertTrue(roller.contains(Rolle.ATTESTERING))
    }

    @Test
    fun `fraAzureGrupper skal returnere unikt sett selv om samme gruppe oppgis flere ganger`() {
        val azureGrupper =
            listOf(
                "5b6745de-b65d-40eb-a6f5-860c8b61c27f", // SAKSBEHANDLER
                "5b6745de-b65d-40eb-a6f5-860c8b61c27f", // SAKSBEHANDLER (duplikat)
                "5b6745de-b65d-40eb-a6f5-860c8b61c27f", // SAKSBEHANDLER (duplikat)
            )

        val roller = Rolle.fraAzureGrupper(azureGrupper)

        assertEquals(1, roller.size)
        assertTrue(roller.contains(Rolle.SAKSBEHANDLER))
    }

    @Test
    fun `skal ha korrekt beskrivelse for hver rolle`() {
        assertEquals("Kan saksbehandle i saksbehandler-løsningen", Rolle.SAKSBEHANDLER.beskrivelse)
        assertEquals("Kan attestere vedtak i saksbehandling-løsningen", Rolle.ATTESTERING.beskrivelse)
        assertEquals("Kan lese og se informasjon i saksbehandling-løsningen", Rolle.LES.beskrivelse)
    }
}
