package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg

/**
 * Steget behandlingen går til når inngangsvilkårene er fullført. Perioder med oppfylte inngangsvilkår må vurderes
 * for sats og trygdetid. Uten rett i noen måned hoppes sats og trygdetid over.
 */
fun utledStegEtterInngangsvilkår(perioder: List<PeriodeMedOppfylteInngangsvilkår>): BehandlingSteg = if (perioder.isEmpty()) BehandlingSteg.VEDTAK_OG_BREV else BehandlingSteg.SATS_OG_TRYGDETID
