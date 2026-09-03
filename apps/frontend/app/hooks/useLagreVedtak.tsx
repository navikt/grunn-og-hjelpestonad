import {useState} from "react";
import type {Vedtak} from "~/komponenter/behandling/vedtak/vedtak";
import {apiCall, type ApiResponse} from "~/api/backend";
import {oppdaterEndringshistorikk} from "~/utils/endringshistorikkEvent";

interface LagreVedtakState {
    oppretter: boolean;
    opprettFeilmelding: string | null;
}

interface LagreVedtakResponse {
    status: string;
}

export function useLagreVedtak() {
    const [state, settState] = useState<LagreVedtakState>({
        oppretter: false,
        opprettFeilmelding: null,
    });

    const lagreVedtak = async (
        behandlingId: string,
        vedtak: Vedtak
    ): Promise<LagreVedtakResponse | undefined> => {
        settState((prev) => ({...prev, oppretter: true, opprettFeilmelding: null}));
        const response: ApiResponse<LagreVedtakResponse> = await apiCall(
            `/vedtak/${behandlingId}/lagre-vedtak`, {
                method: "POST",
                body: JSON.stringify(vedtak),
            }
        );
        settState((prev) => ({...prev, oppretter: false, opprettFeilmelding: response.melding ?? null}));
        if (!response.data && response.melding) {
            return undefined;
        }
        oppdaterEndringshistorikk();
        return response.data;
    };

    return {
        ...state,
        lagreVedtak,
    };
}
