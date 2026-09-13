import {useCallback, useState} from "react";
import {apiCall, type ApiResponse} from "~/api/backend";
import type {BarnetilsynBeregningRequest} from "~/api/generated/types.gen";
import type {Barnetilsynperiode, Beløpsperioder} from "~/komponenter/behandling/vedtak/vedtak";

interface BeløpsperioderState {
    beløpsperioder: Beløpsperioder | null;
    beregnFeilmelding: string | null;
    laster: boolean;
}

export function useHentBeløpsPerioderForVedtak() {
    const [state, settState] = useState<BeløpsperioderState>({
        beløpsperioder: null,
        beregnFeilmelding: null,
        laster: false,
    });

    const hentBeløpsperioder = useCallback(async (
        behandlingId: string | undefined,
        barnetilsynsperioder: Barnetilsynperiode[]
    ) => {

        settState((prev) => ({...prev, laster: true, beregnFeilmelding: null}));
        const request: BarnetilsynBeregningRequest = {
            barnetilsynBeregning: barnetilsynsperioder
                .filter((periode) => periode.periodetype !== undefined)
                .map((periode) => ({
                    datoFra: periode.datoFra,
                    datoTil: periode.datoTil,
                    utgifter: periode.utgifter,
                    barn: periode.barn,
                    periodetype: periode.periodetype!,
                })),
        };
        const response: ApiResponse<Beløpsperioder> = await apiCall(
            `/vedtak/${behandlingId}/beregn`, {
                method: "POST",
                body: JSON.stringify(request),
            }
        );
        settState((prev) => ({
            ...prev,
            beregnFeilmelding: response.melding ?? null,
            beløpsperioder: response.data ?? prev.beløpsperioder,
            laster: false,
        }));
    }, []);

    return {...state, hentBeløpsperioder};
}
