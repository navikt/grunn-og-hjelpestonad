import React, {useState} from "react";
import type { Vedtak } from "~/komponenter/behandling/vedtak/vedtak";
import {useLagreVedtak} from "~/hooks/useLagreVedtak";
import {Button, HStack, Textarea, VStack} from "@navikt/ds-react";
import {useParams} from "react-router";

interface AvslåVedtakProps {
    lagretVedtak: Vedtak | null;
    erLesevisning: boolean;
    låst: boolean;
    onLagreSuksess: () => void;
}

export const AvslåVedtak: React.FC<AvslåVedtakProps> = ({lagretVedtak, erLesevisning, låst, onLagreSuksess}) => {
    const {lagreVedtak} = useLagreVedtak();
    const {behandlingId} = useParams<{ behandlingId: string }>();

    const [begrunnelse, settBegrunnelse] = useState<string>(lagretVedtak?.begrunnelse ?? "");

    const erLåst = erLesevisning || låst;

    async function handleLagreVedtak() {
        if (!behandlingId) return;
        const Vedtak = {
            resultatType: 'AVSLÅTT' as const,
            begrunnelse: begrunnelse,
            barnetilsynperioder: [],
        };
        const response = await lagreVedtak(behandlingId, Vedtak);
        if (response) {
            onLagreSuksess();
        }
    }

    return (
        <VStack gap="space-24">
            <Textarea label={'Begrunnelse'} value={begrunnelse} readOnly={erLåst}
                      onChange={e => settBegrunnelse(e.target.value)}></Textarea>
            {!låst && (
                <HStack>
                    <Button size="medium" onClick={() => handleLagreVedtak()} disabled={erLesevisning}>
                        Lagre vedtak
                    </Button>
                </HStack>
            )}
        </VStack>
    )
};
