import type {
    Barnetilsynperiode as BarnetilsynperiodeDto,
    BeløpsperioderDto,
    VedtakDto,
} from "~/api/generated/types.gen";

type ApiPeriodetype = BarnetilsynperiodeDto['periodetype'];
type ApiAktivitetstypeBarnetilsyn = BarnetilsynperiodeDto['aktivitetstype'];

export type ResultatType = VedtakDto['resultatType'];
export type Periodetype = ApiPeriodetype;
export type AktivitetstypeBarnetilsyn = ApiAktivitetstypeBarnetilsyn;

export const Periodetype = {
    ORDINÆR: 'ORDINÆR',
    INGEN_STØNAD: 'INGEN_STØNAD',
} as const satisfies Record<ApiPeriodetype, ApiPeriodetype>;

export const AktivitetstypeBarnetilsyn = {
    I_ARBEID: 'I_ARBEID',
    FORBIGÅENDE_SYKDOM: 'FORBIGÅENDE_SYKDOM',
    IKKE_RELEVANT: 'IKKE_RELEVANT',
} as const satisfies Record<ApiAktivitetstypeBarnetilsyn, ApiAktivitetstypeBarnetilsyn>;

// Redigeringsmodellene under er skjematilstand: perioden har ingen persistens-id
// før den er lagret, og periodetype/aktivitetstype er tomme til saksbehandleren
// har fylt dem ut. Ellers følger de API-kontrakten.
export type Barnetilsynperiode = Omit<BarnetilsynperiodeDto, 'id' | 'periodetype' | 'aktivitetstype'> & {
    id?: string;
    periodetype: Periodetype | undefined;
    aktivitetstype: AktivitetstypeBarnetilsyn | undefined;
};

export type Vedtak = Omit<VedtakDto, 'barnetilsynperioder'> & {
    barnetilsynperioder: Barnetilsynperiode[];
};

export type Beløpsperioder = BeløpsperioderDto[];
