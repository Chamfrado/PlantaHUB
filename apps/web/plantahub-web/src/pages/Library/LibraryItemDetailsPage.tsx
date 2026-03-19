import { ArrowLeft, Download, FileText, Loader2 } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useToast } from '../../components/ui/use-toast';
import { getApiErrorMessage } from '../../lib/api-error';
import { getDownloadDetails } from '../../services/download.service';
import { getMyLibrary } from '../../services/library.service';
import type { DownloadResponseDTO } from '../../types/api/download';
import type { LibraryProductDTO } from '../../types/api/library';

export default function LibraryItemDetailsPage() {
  const { productId = '', planTypeCode = '' } = useParams();
  const { showToast } = useToast();

  const [library, setLibrary] = useState<LibraryProductDTO[]>([]);
  const [download, setDownload] = useState<DownloadResponseDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [downloadingAll, setDownloadingAll] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);
        setError(null);

        const [libraryResponse, downloadResponse] = await Promise.all([
          getMyLibrary(),
          getDownloadDetails(productId, planTypeCode),
        ]);

        setLibrary(libraryResponse);
        setDownload(downloadResponse);
      } catch (error) {
        console.error(error);
        setError(getApiErrorMessage(error, 'Não foi possível carregar os arquivos desta planta.'));
      } finally {
        setLoading(false);
      }
    }

    if (productId && planTypeCode) {
      void loadData();
    }
  }, [productId, planTypeCode]);

  const product = useMemo(
    () => library.find(item => item.productId === productId) ?? null,
    [library, productId]
  );

  const planType = useMemo(
    () => product?.planTypes.find(item => item.code === planTypeCode) ?? null,
    [product, planTypeCode]
  );

  async function handleDownloadAll() {
    if (!download?.files.length) return;

    try {
      setDownloadingAll(true);

      download.files.forEach(file => {
        window.open(file.url, '_blank', 'noopener,noreferrer');
      });

      showToast({
        variant: 'success',
        title: 'Downloads iniciados',
        description: 'Os arquivos desta planta foram abertos para download.',
      });
    } catch (error) {
      console.error(error);
      showToast({
        variant: 'error',
        title: 'Erro ao iniciar downloads',
        description: 'Tente novamente.',
      });
    } finally {
      setDownloadingAll(false);
    }
  }

  if (loading) {
    return (
      <section className="min-h-[calc(100vh-64px)] bg-neutral-50">
        <div className="mx-auto max-w-5xl px-6 py-12">
          <div className="flex items-center gap-3 text-neutral-600">
            <Loader2 className="h-5 w-5 animate-spin" />
            Carregando arquivos...
          </div>
        </div>
      </section>
    );
  }

  if (error || !product || !planType || !download) {
    return (
      <section className="min-h-[calc(100vh-64px)] bg-neutral-50">
        <div className="mx-auto max-w-5xl px-6 py-12">
          <div className="rounded-3xl border border-red-200 bg-white p-8 shadow-sm">
            <h1 className="text-2xl font-extrabold text-neutral-900">
              Não foi possível abrir este item
            </h1>
            <p className="mt-2 text-sm text-red-600">
              {error ?? 'Item não encontrado na sua biblioteca.'}
            </p>

            <Link
              to="/biblioteca"
              className="mt-6 inline-flex items-center gap-2 rounded-xl border border-neutral-300 bg-white px-4 py-2 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-50"
            >
              <ArrowLeft className="h-4 w-4" />
              Voltar para biblioteca
            </Link>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="min-h-[calc(100vh-64px)] bg-neutral-50">
      <div className="mx-auto max-w-5xl px-6 py-12">
        <Link
          to="/biblioteca"
          className="inline-flex items-center gap-2 text-sm font-semibold text-neutral-600 transition hover:text-primary-600"
        >
          <ArrowLeft className="h-4 w-4" />
          Voltar para biblioteca
        </Link>

        <div className="mt-6 overflow-hidden rounded-3xl border border-neutral-200 bg-white shadow-sm">
          <div className="grid gap-0 md:grid-cols-[280px_minmax(0,1fr)]">
            <div className="min-h-220px bg-neutral-100">
              {product.heroImageUrl ? (
                <img
                  src={product.heroImageUrl}
                  alt={product.name}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="flex h-full items-center justify-center text-sm text-neutral-400">
                  Sem imagem
                </div>
              )}
            </div>

            <div className="p-6 md:p-8">
              <div className="inline-flex rounded-full bg-orange-50 px-3 py-1 text-xs font-semibold text-primary-600">
                {product.category}
              </div>

              <h1 className="mt-4 text-3xl font-extrabold text-neutral-900">{product.name}</h1>

              <div className="mt-3 rounded-2xl border border-green-200 bg-green-50 px-4 py-3">
                <div className="text-sm font-bold text-green-800">{planType.name}</div>
                <div className="mt-1 text-xs text-green-700">
                  {download.files.length} arquivo(s) disponível(is) para download
                </div>
              </div>

              <div className="mt-6 flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={handleDownloadAll}
                  disabled={downloadingAll || download.files.length === 0}
                  className="inline-flex items-center gap-2 rounded-xl bg-primary-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary-600 disabled:opacity-50"
                >
                  <Download className="h-4 w-4" />
                  {downloadingAll ? 'Iniciando...' : 'Baixar todos'}
                </button>
              </div>

              <div className="mt-8 space-y-4">
                {download.files.map(file => (
                  <div
                    key={`${file.storageKey}-${file.filename}`}
                    className="flex flex-col gap-4 rounded-2xl border border-neutral-200 bg-neutral-50 p-4 md:flex-row md:items-center md:justify-between"
                  >
                    <div className="min-w-0">
                      <div className="inline-flex items-center gap-2 text-sm font-bold text-neutral-900">
                        <FileText className="h-4 w-4 text-primary-600" />
                        <span className="truncate">{file.filename}</span>
                      </div>

                      <div className="mt-1 text-xs text-neutral-500">
                        {formatFileSize(file.sizeBytes)}
                      </div>
                    </div>

                    <a
                      href={file.url}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center justify-center gap-2 rounded-xl border border-neutral-300 bg-white px-4 py-2 text-sm font-semibold text-neutral-800 transition hover:bg-neutral-100"
                    >
                      <Download className="h-4 w-4" />
                      Baixar arquivo
                    </a>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function formatFileSize(sizeBytes: number) {
  if (sizeBytes < 1024) return `${sizeBytes} B`;
  if (sizeBytes < 1024 * 1024) return `${(sizeBytes / 1024).toFixed(1)} KB`;
  if (sizeBytes < 1024 * 1024 * 1024) return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(sizeBytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}
