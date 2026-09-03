import React from "react";
import {
  AktivitetstypeBarnetilsyn,
  type Barnetilsynperiode,
  Periodetype,
} from "~/komponenter/behandling/vedtak/vedtak";
import {
  Button,
  HStack,
  MonthPicker,
  Select,
  Table,
  TextField,
  UNSAFE_Combobox,
} from "@navikt/ds-react";
import {
  beregnAlder,
  formaterYearMonthStringTilNorskDato,
  månedStringTilYearMonth,
} from "~/utils/utils";
import { TrashIcon, PlusIcon } from "@navikt/aksel-icons";
import { useParams } from "react-router";
import type { Barn } from "~/hooks/useHentBarn";

export const BarnetilsynperiodeValg: React.FC<{
  perioder: Barnetilsynperiode[];
  settPerioder: React.Dispatch<React.SetStateAction<Barnetilsynperiode[]>>;
  erLesevisning: boolean;
  erRevurdering?: boolean;
  barn: Barn[];
}> = ({ perioder, settPerioder, erLesevisning, erRevurdering = false, barn }) => {
  const { behandlingId } = useParams<{ behandlingId: string }>();

  const barnOptions = barn.map((b) => ({
    label: `${b.navn} (${beregnAlder(b.fødselsdato)} år)`,
    value: b.id,
  }));

  const getSelectedBarnOptions = (barnValues: string[]) => {
    return barnValues
      .map((value) => barnOptions.find((opt) => opt.value === value))
      .filter((opt): opt is { label: string; value: string } => opt !== undefined);
  };

  function handlePeriodeChange(
    index: number,
    felt: keyof Barnetilsynperiode,
    value: string | number | string[]
  ) {
    settPerioder((prev) =>
      prev.map((p, i) => {
        if (i !== index) return p;
        if (felt === "periodetype" && value === Periodetype.INGEN_STØNAD) {
          return {
            ...p,
            periodetype: value,
            barn: [],
            utgifter: 0,
            aktivitetstype: AktivitetstypeBarnetilsyn.IKKE_RELEVANT,
          };
        }
        return { ...p, [felt]: value };
      })
    );
  }

  function handleBarnChange(index: number, option: string, isSelected: boolean) {
    settPerioder((prev) =>
      prev.map((p, i) =>
        i === index
          ? { ...p, barn: isSelected ? [...p.barn, option] : p.barn.filter((b) => b !== option) }
          : p
      )
    );
  }

  function handlePeriodeMonthChange(index: number, felt: "datoFra" | "datoTil", value: string) {
    settPerioder((prev) => prev.map((p, i) => (i === index ? { ...p, [felt]: value } : p)));
  }

  function leggTilPeriode() {
    if (!behandlingId) return;
    settPerioder((prev) => [
      ...prev,
      {
        datoFra: "",
        datoTil: "",
        utgifter: 0,
        barn: [],
        periodetype: undefined,
        aktivitetstype: undefined,
      },
    ]);
  }

  function leggTilPeriodeEtter(index: number) {
    settPerioder((prev) => {
      const nyPeriode: Barnetilsynperiode = {
        datoFra: "",
        datoTil: "",
        utgifter: 0,
        barn: [],
        periodetype: undefined,
        aktivitetstype: undefined,
      };
      const nyePerioder = [...prev];
      nyePerioder.splice(index + 1, 0, nyPeriode);
      return nyePerioder;
    });
  }

  function slettPeriode(index: number) {
    settPerioder((prev) => prev.filter((_, i) => i !== index));
  }

  return (
    <>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col" textSize="small">
              Periodetype
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" textSize="small">
              Aktivitet
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" textSize="small">
              Periode fra og med
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" textSize="small">
              Periode til og med
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" textSize="small">
              Velg barn
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" textSize="small">
              Barn
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" textSize="small">
              Utgifter
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" textSize="small">
              {" "}
            </Table.HeaderCell>
            <Table.HeaderCell scope="col" textSize="small">
              {" "}
            </Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {perioder.map((periode, index) => (
            <Table.Row key={`${index}-${periode.datoFra}-${periode.datoTil}`}>
              <Table.DataCell>
                <Select
                  size="small"
                  label="Periodetype"
                  value={periode.periodetype}
                  onChange={(e) =>
                    handlePeriodeChange(index, "periodetype", e.target.value as Periodetype)
                  }
                  hideLabel
                  disabled={erLesevisning}
                >
                  <option value="">Velg</option>
                  <option value={Periodetype.ORDINÆR}>Ordinær</option>
                  <option value={Periodetype.INGEN_STØNAD}>Ingen stønad</option>
                </Select>
              </Table.DataCell>
              {periode.periodetype == Periodetype.INGEN_STØNAD ? (
                <Table.DataCell></Table.DataCell>
              ) : (
                <Table.DataCell>
                  <Select
                    size="small"
                    label="Aktivitet"
                    value={periode.aktivitetstype}
                    onChange={(e) =>
                      handlePeriodeChange(
                        index,
                        "aktivitetstype",
                        e.target.value as AktivitetstypeBarnetilsyn
                      )
                    }
                    hideLabel
                    disabled={erLesevisning}
                  >
                    <option value="">Velg</option>
                    <option value={AktivitetstypeBarnetilsyn.I_ARBEID}>I arbeid</option>
                    <option value={AktivitetstypeBarnetilsyn.FORBIGÅENDE_SYKDOM}>
                      Forbigående sykdom
                    </option>
                  </Select>
                </Table.DataCell>
              )}
              <Table.DataCell>
                <MonthPicker
                  selected={periode.datoFra ? new Date(periode.datoFra) : undefined}
                  onMonthSelect={(date) =>
                    handlePeriodeMonthChange(
                      index,
                      "datoFra",
                      date
                        ? månedStringTilYearMonth(
                            date.toLocaleString("nb-NO", {
                              month: "long",
                              year: "numeric",
                            })
                          )
                        : ""
                    )
                  }
                >
                  <MonthPicker.Input
                    size="small"
                    label="Fra og med"
                    disabled={erLesevisning || (erRevurdering && index === 0)}
                    hideLabel
                    value={formaterYearMonthStringTilNorskDato(periode.datoFra)}
                    onChange={(e) => handlePeriodeMonthChange(index, "datoFra", e.target.value)}
                    description="Format: mm.åååå"
                  />
                </MonthPicker>
              </Table.DataCell>
              <Table.DataCell>
                <MonthPicker
                  selected={periode.datoTil ? new Date(periode.datoTil) : undefined}
                  onMonthSelect={(date) =>
                    handlePeriodeMonthChange(
                      index,
                      "datoTil",
                      date
                        ? månedStringTilYearMonth(
                            date.toLocaleString("nb-NO", {
                              month: "long",
                              year: "numeric",
                            })
                          )
                        : ""
                    )
                  }
                  fromDate={periode.datoFra ? new Date(periode.datoFra) : undefined}
                >
                  <MonthPicker.Input
                    size="small"
                    label="Til og med"
                    hideLabel
                    disabled={erLesevisning}
                    value={formaterYearMonthStringTilNorskDato(periode.datoTil)}
                    onChange={(e) => handlePeriodeMonthChange(index, "datoTil", e.target.value)}
                    description="Format: mm.åååå"
                  />
                </MonthPicker>
              </Table.DataCell>
              {periode.periodetype == Periodetype.INGEN_STØNAD ? (
                <>
                  <Table.DataCell></Table.DataCell>
                  <Table.DataCell></Table.DataCell>
                  <Table.DataCell></Table.DataCell>
                </>
              ) : (
                <>
                  <Table.DataCell>
                    <UNSAFE_Combobox
                      size="small"
                      label={"Barn"}
                      options={barnOptions}
                      isMultiSelect
                      hideLabel
                      disabled={erLesevisning}
                      placeholder={"Velg barn"}
                      selectedOptions={getSelectedBarnOptions(periode.barn)}
                      onToggleSelected={(option, isSelected) => {
                        handleBarnChange(index, option, isSelected);
                      }}
                    />
                  </Table.DataCell>
                  <Table.DataCell>
                    <span>{periode.barn.length}</span>
                  </Table.DataCell>
                  <Table.DataCell>
                    <TextField
                      style={{
                        width: "6rem",
                      }}
                      size="small"
                      label="Utgifter"
                      disabled={erLesevisning}
                      value={periode.utgifter}
                      onChange={(e) =>
                        handlePeriodeChange(index, "utgifter", Number(e.target.value))
                      }
                      hideLabel
                    />
                  </Table.DataCell>
                </>
              )}
              <Table.DataCell>
                <Button
                  onClick={() => leggTilPeriodeEtter(index)}
                  variant="tertiary"
                  disabled={erLesevisning}
                  icon={<PlusIcon fontSize="1.5rem" />}
                ></Button>
              </Table.DataCell>
              <Table.DataCell>
                <Button
                  onClick={() => slettPeriode(index)}
                  variant="tertiary"
                  disabled={erLesevisning || (erRevurdering && index === 0)}
                  icon={<TrashIcon fontSize="1.5rem" />}
                ></Button>
              </Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
      <HStack>
        <Button onClick={leggTilPeriode} variant="secondary" disabled={erLesevisning}>
          Legg til vedtaksperiode
        </Button>
      </HStack>
    </>
  );
};
