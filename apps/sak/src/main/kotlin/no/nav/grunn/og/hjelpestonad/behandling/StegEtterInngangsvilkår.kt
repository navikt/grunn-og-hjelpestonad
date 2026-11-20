package no.nav.grunn.og.hjelpestonad.behandling

/**
 * Steget behandlingen går til når inngangsvilkårene er fullført. Perioder med oppfylte inngangsvilkår må vurderes
 * for sats og trygdetid. Uten rett i noen måned hoppes sats og trygdetid over.
 */
fun utledStegEtterInngangsvilkår(harPerioderMedOppfylteInngangsvilkår: Boolean): BehandlingSteg = if (harPerioderMedOppfylteInngangsvilkår) BehandlingSteg.SATS_OG_TRYGDETID else BehandlingSteg.VEDTAK_OG_BREV
