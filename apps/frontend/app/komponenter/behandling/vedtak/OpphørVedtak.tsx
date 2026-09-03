import React, {useState} from "react";
import type { Vedtak } from "~/komponenter/behandling/vedtak/vedtak";
import {useLagreVedtak} from "~/hooks/useLagreVedtak";
import {Button, HStack, MonthPicker, Textarea, useMonthpicker, VStack} from "@navikt/ds-react";
import {useParams} from "react-router";
import {format} from "date-fns";

interface OpphørVedtakProps {
    lagretVedtak: Vedtak | null;
    erLesevisning: boolean;
    låst: boolean;
    onLagreSuksess: () => void;
}

export const OpphørVedtak: React.FC<OpphørVedtakProps> = ({lagretVedtak, erLesevisning, låst, onLagreSuksess}) => {
    const {lagreVedtak} = useLagreVedtak();
    const [monthPickerError, setMonthPickerError] = useState(false);
    const {behandlingId} = useParams<{ behandlingId: string }>();
    const {monthpickerProps, inputProps, selectedMonth} = useMonthpicker({
        defaultSelected: lagretVedtak?.opphørFom ? new Date(lagretVedtak.opphørFom) : undefined,
        onValidate: (val) => {
            setMonthPickerError(!val.isValidMonth);
        },
    });

    const [begrunnelse, settBegrunnelse] = useState<string>(lagretVedtak?.begrunnelse ?? "");

    const erLåst = erLesevisning || låst;

    async function handleLagreVedtak() {
        if (!behandlingId || !selectedMonth) return;
        const Vedtak = {
            resultatType: 'OPPHØR' as const,
            begrunnelse: begrunnelse,
            barnetilsynperioder: [],
            opphørFom: format(selectedMonth, 'yyyy-MM')
        };
        const response = await lagreVedtak(behandlingId, Vedtak);
        if (response) {
            onLagreSuksess();
        }
    }

    return (
        <VStack gap="space-24">
            <MonthPicker {...monthpickerProps}>
                <MonthPicker.Input
                    {...inputProps}
                    label="Opphør fra og med"
                    error={monthPickerError && "Du må velge måned"}
                    readOnly={erLåst}
                />
            </MonthPicker>
            <Textarea label={'Begrunnelse'} value={begrunnelse} disabled={erLåst}
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
