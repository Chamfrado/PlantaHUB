import { AlertTriangle } from 'lucide-react';
import { PrimaryButton, SecondaryButton, SelectInput } from '../ui/primitives';

export type FolderMapping = {
  folder: string;
  collectionCode: string;
  fileCount: number;
};

type Props = {
  mappings: FolderMapping[];
  collections: string[];
  onChange: (next: FolderMapping[]) => void;
  onConfirm: () => void;
  onCancel: () => void;
};

/**
 * Mostra para onde cada pasta vai, antes de qualquer byte subir.
 *
 * É o detalhe que faz "jogar a pasta ARCH inteira" funcionar de verdade: o acerto
 * automático por nome é conveniência, mas o destino final é sempre explícito e editável.
 * Sem esta confirmação, um erro de mapeamento só apareceria depois de duzentos arquivos
 * terem ido para o lugar errado.
 */
export default function FolderMappingTable({
  mappings,
  collections,
  onChange,
  onConfirm,
  onCancel,
}: Props) {
  const unmapped = mappings.filter(m => !m.collectionCode);
  const skippedFiles = unmapped.reduce((sum, m) => sum + m.fileCount, 0);
  const totalFiles = mappings.reduce((sum, m) => sum + m.fileCount, 0);

  return (
    <section className="rounded-2xl border border-neutral-200 bg-white p-6">
      <h3 className="text-base font-extrabold text-neutral-900">Confirme o destino</h3>
      <p className="mt-1 text-xs text-neutral-500">
        Cada pasta do primeiro nível será enviada para a coleção escolhida aqui.
      </p>

      <div className="mt-4 space-y-2">
        {mappings.map((mapping, index) => (
          <div
            key={mapping.folder}
            className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-neutral-200 px-4 py-3"
          >
            <div className="min-w-0">
              <code className="font-bold text-neutral-900">{mapping.folder}/</code>
              <span className="ml-2 text-xs text-neutral-500">
                {mapping.fileCount} arquivo(s)
              </span>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-xs text-neutral-400">para</span>
              <SelectInput
                value={mapping.collectionCode}
                onChange={e => {
                  const next = [...mappings];
                  next[index] = { ...mapping, collectionCode: e.target.value };
                  onChange(next);
                }}
              >
                <option value="">Não enviar</option>
                {collections.map(code => (
                  <option key={code} value={code}>
                    {code}
                  </option>
                ))}
              </SelectInput>
            </div>
          </div>
        ))}
      </div>

      {skippedFiles > 0 ? (
        <div className="mt-4 flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>
            {skippedFiles} arquivo(s) não serão enviados porque a pasta deles não tem
            coleção escolhida. Escolha uma coleção acima, ou crie a coleção antes.
          </span>
        </div>
      ) : null}

      <div className="mt-5 flex justify-end gap-3">
        <SecondaryButton onClick={onCancel}>Cancelar</SecondaryButton>
        <PrimaryButton onClick={onConfirm} disabled={skippedFiles === totalFiles}>
          Adicionar {totalFiles - skippedFiles} arquivo(s) à fila
        </PrimaryButton>
      </div>
    </section>
  );
}
