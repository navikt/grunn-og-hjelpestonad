import {useEffect, useState} from "react";
import {apiCall, type ApiResponse} from "~/api/backend";
import type {Vedtak} from "~/komponenter/behandling/vedtak/vedtak";

interface VedtakState {
    vedtak: Vedtak | null;
    melding: string | null;
    laster: boolean;
}

export function useHentVedtak(behandlingId: string | undefined) {
    const [state, settState] = useState<VedtakState>({
        vedtak: null,
        melding: null,
        laster: true,
    });

    useEffect(() => {
        const hentVedtakForBehandling = async (
            behandlingId: string
        ): Promise<ApiResponse<Vedtak>> => {
            return apiCall(`/vedtak/${behandlingId}/hent-vedtak`, {
                method: "GET",
            });
        };

        const hentVedtak = async () => {
            if (!behandlingId) {
                settState((prev) => ({
                    ...prev,
                    melding: "Mangler behandlingId",
                    laster: false,
                }));
                return;
            }

            settState((prev) => ({...prev, melding: null, laster: true}));

            const response = await hentVedtakForBehandling(behandlingId);

            settState((prev) => ({
                ...prev,
                vedtak: response.data ?? null,
                laster: false,
            }));
        };

        hentVedtak();
    }, [behandlingId]);

    return state;
}
