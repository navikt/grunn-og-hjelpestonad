import {useEffect, useState} from "react";
import {apiCall, type ApiResponse} from "~/api/backend";
import type {Barnetilsynperiode} from "~/komponenter/behandling/vedtak/vedtak";

interface HistoriskVedtakState {
    historiskVedtak: HistoriskVedtakResponse | null;
    melding: string | null;
    laster: boolean;
}

export interface HistoriskVedtakResponse{
    barnetilsynperioder: Barnetilsynperiode[] | null;
    fraErFørTidligsteVedtak: boolean;
}

export function useHentVedtakHistorikk(behandlingId: string | undefined, fra: string | null) {
    const [state, settState] = useState<HistoriskVedtakState>({
        historiskVedtak: null,
        melding: null,
        laster: false,
    });

    useEffect(() => {
        const hentHistoriskVedtakForBehandling = async (
            behandlingId: string,
            fra: string
        ): Promise<ApiResponse<HistoriskVedtakResponse>> => {
            return apiCall(`/vedtak/${behandlingId}/historikk/${fra}`, {
                method: "GET",
            });
        };

        const hentVedtakHistorikk = async () => {
            if (!behandlingId || !fra) {
                settState((prev) => ({
                    ...prev,
                    historiskVedtak: null,
                    laster: false,
                }));
                return;
            }

            settState((prev) => ({...prev, melding: null, laster: true}));

            const response = await hentHistoriskVedtakForBehandling(behandlingId, fra);

            settState((prev) => ({
                ...prev,
                historiskVedtak: response.data ?? null,
                laster: false,
            }));
        };

        hentVedtakHistorikk();
    }, [behandlingId, fra]);

    return state;
}
