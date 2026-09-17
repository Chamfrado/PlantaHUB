import { FolderUp, Upload } from 'lucide-react';
import { useRef, useState } from 'react';
import { subPathOf, topFolderOf } from '../../../hooks/useUploadQueue';
import { SecondaryButton } from '../ui/primitives';
import FolderMappingTable, { type FolderMapping } from './FolderMappingTable';

export type ResolvedUpload = {
  file: File;
  collectionCode: string;
  relativePath?: string;
};

type Props = {
  collections: string[];
  /**
   * Para onde vão os arquivos soltos. `null` desabilita o envio.
   *
   * Não tem padrão: qualquer escolha automática seria uma coleção que ninguém pediu, e o
   * erro só apareceria depois, com os arquivos já no lugar errado.
   */
  destination: { code: string; label: string } | null;
  onEnqueue: (entries: ResolvedUpload[]) => void;
};

/**
 * Seleção de arquivos ou de uma pasta inteira.
 *
 * O destino é sempre explícito. Para arquivos soltos, ele vem de fora — a coleção que o
 * administrador escolheu. Para pastas, o mapeamento pasta→coleção é **mostrado e
 * confirmado antes** de qualquer byte subir. Adivinhar em silêncio significaria descobrir
 * o erro só depois de enviar duzentos arquivos para o lugar errado.
 */
export default function UploadDropzone({ collections, destination, onEnqueue }: Props) {
  const fileInput = useRef<HTMLInputElement>(null);
  const folderInput = useRef<HTMLInputElement>(null);

  const [pendingFolder, setPendingFolder] = useState<File[] | null>(null);
  const [mappings, setMappings] = useState<FolderMapping[]>([]);

  function handleFiles(fileList: FileList | null) {
    if (!fileList || fileList.length === 0 || !destination) return;

    onEnqueue(
      Array.from(fileList).map(file => ({ file, collectionCode: destination.code }))
    );

    if (fileInput.current) fileInput.current.value = '';
  }

  function handleFolder(fileList: FileList | null) {
    if (!fileList || fileList.length === 0) return;

    const files = Array.from(fileList);

    // Casa cada pasta de topo com uma coleção de mesmo código, sem diferenciar maiúsculas.
    // O acerto é conveniência; a decisão continua sendo explícita e editável.
    const folders = Array.from(new Set(files.map(topFolderOf).filter(Boolean) as string[]));

    setMappings(
      folders.map(folder => ({
        folder,
        collectionCode:
          collections.find(code => code.toLowerCase() === folder.toLowerCase()) ?? '',
        fileCount: files.filter(file => topFolderOf(file) === folder).length,
      }))
    );

    setPendingFolder(files);

    if (folderInput.current) folderInput.current.value = '';
  }

  function confirmFolder() {
    if (!pendingFolder) return;

    const byFolder = new Map(mappings.map(m => [m.folder, m.collectionCode]));

    const entries = pendingFolder
      .map(file => {
        const folder = topFolderOf(file);
        const collectionCode = folder ? byFolder.get(folder) : destination?.code;

        // Pasta não mapeada é pulada, e a tabela já avisou quantos arquivos isso afeta.
        if (!collectionCode) return null;

        return { file, collectionCode, relativePath: subPathOf(file) };
      })
      .filter(Boolean) as ResolvedUpload[];

    onEnqueue(entries);
    setPendingFolder(null);
    setMappings([]);
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-3 rounded-2xl border border-dashed border-neutral-300 bg-white p-6">
        <SecondaryButton onClick={() => fileInput.current?.click()} disabled={!destination}>
          <Upload className="h-4 w-4" /> Selecionar arquivos
        </SecondaryButton>

        <SecondaryButton onClick={() => folderInput.current?.click()}>
          <FolderUp className="h-4 w-4" /> Selecionar pasta
        </SecondaryButton>

        <p className="w-full text-xs text-neutral-500">
          {destination ? (
            <>
              Os arquivos vão para <strong className="text-neutral-700">{destination.label}</strong>.
            </>
          ) : (
            'Escolha a coleção de destino acima para poder enviar arquivos soltos.'
          )}{' '}
          Ao enviar uma pasta, cada subpasta do primeiro nível vira uma coleção — você
          confirma o mapeamento antes do envio começar.
        </p>

        <input
          ref={fileInput}
          type="file"
          multiple
          hidden
          onChange={e => handleFiles(e.target.files)}
        />

        {/* `webkitdirectory` não está nos tipos padrão do React; a declaração vive em
            types/dom.d.ts para não precisar de `any` aqui. */}
        <input
          ref={folderInput}
          type="file"
          multiple
          hidden
          webkitdirectory=""
          directory=""
          onChange={e => handleFolder(e.target.files)}
        />
      </div>

      {pendingFolder ? (
        <FolderMappingTable
          mappings={mappings}
          collections={collections}
          onChange={setMappings}
          onConfirm={confirmFolder}
          onCancel={() => {
            setPendingFolder(null);
            setMappings([]);
          }}
        />
      ) : null}
    </div>
  );
}
