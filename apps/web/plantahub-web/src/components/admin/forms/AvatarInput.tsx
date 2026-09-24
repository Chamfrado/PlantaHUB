import { ImagePlus, Trash2, UserRound } from 'lucide-react';
import { useRef, useState } from 'react';
import { getApiErrorMessage } from '../../../lib/api-error';
import { uploadContentImage } from '../../../services/admin/upload.service';
import { SecondaryButton } from '../ui/primitives';

type Props = {
  productId: string;
  value: string | null | undefined;
  onChange: (url: string | null) => void;
  alt?: string;
};

/**
 * Foto redonda de uma pessoa citada no conteúdo (ex.: autor de um depoimento).
 *
 * Sobe direto ao ser escolhida: a URL só entra no conteúdo quando o admin clicar em
 * "Salvar conteúdo", como qualquer outro campo da aba.
 */
export default function AvatarInput({ productId, value, onChange, alt }: Props) {
  const fileInput = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleFile(file: File) {
    setUploading(true);
    setError(null);
    try {
      onChange(await uploadContentImage(productId, file));
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível enviar a foto.'));
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="flex h-14 w-14 shrink-0 items-center justify-center overflow-hidden rounded-full bg-neutral-100 text-neutral-400">
        {value ? (
          <img src={value} alt={alt || 'Foto'} className="h-full w-full object-cover" />
        ) : (
          <UserRound className="h-6 w-6" aria-hidden />
        )}
      </div>

      <SecondaryButton
        type="button"
        disabled={uploading}
        onClick={() => fileInput.current?.click()}
      >
        <ImagePlus className="h-4 w-4" />
        {uploading ? 'Enviando…' : value ? 'Trocar foto' : 'Adicionar foto'}
      </SecondaryButton>

      {value && !uploading ? (
        <button
          type="button"
          onClick={() => onChange(null)}
          className="inline-flex items-center gap-1 text-sm font-semibold text-red-600 hover:text-red-700"
        >
          <Trash2 className="h-4 w-4" /> Remover
        </button>
      ) : null}

      <input
        ref={fileInput}
        type="file"
        hidden
        accept="image/png,image/jpeg,image/webp,image/avif"
        aria-label="Selecionar foto"
        onChange={event => {
          const file = event.target.files?.[0];
          event.target.value = '';
          if (file) void handleFile(file);
        }}
      />

      {error ? <p className="w-full text-xs font-semibold text-red-600">{error}</p> : null}
    </div>
  );
}
