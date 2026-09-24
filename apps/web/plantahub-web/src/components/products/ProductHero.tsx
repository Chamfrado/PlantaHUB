import {
  BadgeCheck,
  Download,
  FileText,
  Headset,
  Ruler,
  ShieldCheck,
  ShoppingCart,
  SlidersHorizontal,
  Zap,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import type { ProductDetailView } from '../../types/product-view';
import { formatCurrency } from '../../utils/format';

type Props = {
  product: ProductDetailView;
};

export default function ProductHero({ product }: Props) {
  const title = product.headline;
  const description = product.description ?? '';
  const subtitle = product.subheadline ?? product.shortDescription ?? '';

  const gallery = useMemo(() => {
    const list = [product.heroImageUrl, ...product.galleryImageUrls].filter(Boolean) as string[];
    // avoid duplicates
    return Array.from(new Set(list));
  }, [product.heroImageUrl, product.galleryImageUrls]);

  const [selected, setSelected] = useState<string | null>(null);

  // Derivada, e nao um estado que espelha a galeria: ao trocar de produto sem desmontar a
  // pagina, um estado guardado continuaria apontando para a foto do produto anterior.
  const activeImg = selected && gallery.includes(selected) ? selected : gallery[0];

  const price = product.basePriceCents !== null ? formatCurrency(product.basePriceCents) : null;

  return (
    <section className="bg-white">
      <div className="max-w-7xl mx-auto px-6 pt-8 pb-10">
        {/* Breadcrumb */}
        <div className="text-sm text-brand-muted">
          <Link className="hover:text-brand-black cursor-pointer" to="/">
            Home
          </Link>
          <span className="mx-2">/</span>
          <Link
            className="hover:text-brand-black"
            to={`/produtos?category=${encodeURIComponent(product.category)}`}
          >
            {product.categoryName}
          </Link>
          <span className="mx-2">/</span>
          <span className="text-brand-black font-semibold">{title}</span>
        </div>

        <div className="mt-6 grid gap-8 lg:grid-cols-2 lg:items-start">
          {/* LEFT — Gallery */}
          <div>
            <div className="rounded-2xl border border-neutral-200 overflow-hidden bg-brand-light">
              {activeImg ? (
                <img
                  src={activeImg}
                  alt={title}
                  className="w-full h-90 md:h-90 object-cover"
                  loading="lazy"
                />
              ) : (
                <div className="w-full h-90 md:h-90 bg-neutral-100" />
              )}
            </div>

            {/* thumbnails */}
            {/* Todas as miniaturas, com rolagem horizontal quando nao cabem. Cortar em
                cinco descartaria em silencio a sexta imagem que o admin subiu. */}
            {gallery.length > 1 ? (
              <div
                className="mt-4 flex gap-3 overflow-x-auto pb-2"
                role="group"
                aria-label="Imagens do produto"
              >
                {gallery.map((src, index) => {
                  const active = src === activeImg;
                  return (
                    <button
                      key={src}
                      onClick={() => setSelected(src)}
                      className={[
                        'h-16 w-24 shrink-0 rounded-xl overflow-hidden border transition',
                        active
                          ? 'border-primary-500 ring-2 ring-primary-200'
                          : 'border-neutral-200 hover:border-neutral-300',
                      ].join(' ')}
                      // Um rotulo identico em todos os botoes faz o leitor de tela anunciar
                      // "Selecionar imagem" varias vezes seguidas, sem dizer qual e qual.
                      aria-label={`Ver imagem ${index + 1} de ${gallery.length}`}
                      aria-pressed={active}
                    >
                      <img src={src} alt="" className="h-full w-full object-cover" loading="lazy" />
                    </button>
                  );
                })}
              </div>
            ) : null}
          </div>

          {/* RIGHT — Purchase panel */}
          <div>
            {/* badges */}
            {product.tags.length > 0 ? (
              <div className="flex flex-wrap gap-2">
                {product.tags.slice(0, 3).map((tag, idx) => (
                  <BadgePill key={tag} tone={idx === 0 ? 'orange' : 'green'}>
                    {tag.toUpperCase()}
                  </BadgePill>
                ))}
              </div>
            ) : null}

            <h1 className="mt-3 text-4xl font-extrabold text-brand-black">{title}</h1>

            {subtitle ? <p className="mt-2 text-brand-muted font-semibold">{subtitle}</p> : null}

            {description ? (
              <p className="mt-3 text-brand-muted leading-relaxed">{description}</p>
            ) : null}

            {/* quick stats row */}
            <div className="mt-6 grid grid-cols-2 sm:grid-cols-4 gap-4 border-y border-neutral-200 py-5">
              <InfoMini
                icon={<Ruler className="h-5 w-5 text-primary-500" />}
                label="Área"
                value={product.areaM2 !== null ? `${product.areaM2} m²` : '—'}
              />
              <InfoMini
                icon={<FileText className="h-5 w-5 text-primary-500" />}
                label="Formatos"
                value={formatFormats(product.fileFormats)}
              />
              <InfoMini
                icon={<Zap className="h-5 w-5 text-primary-500" />}
                label="Entrega"
                value={product.delivery ? 'Imediata' : '—'}
              />
              <InfoMini
                icon={<SlidersHorizontal className="h-5 w-5 text-primary-500" />}
                label="Customizável"
                value={product.customizable ? 'Sim' : 'Não'}
              />
            </div>

            {/* price */}
            <div className="mt-5">
              {price ? (
                <>
                  {/* O valor e o da colecao mais barata; quem monta a compra e o seletor
                      logo abaixo. Sem o rotulo, o numero passaria por preco de levar tudo. */}
                  <div className="text-sm font-semibold text-brand-muted">A partir de</div>
                  <div className="text-4xl font-extrabold text-brand-black">{price}</div>
                </>
              ) : (
                <div className="text-xl font-extrabold text-brand-black">Consulte valores</div>
              )}

              <p className="mt-2 text-sm text-brand-muted">
                Compra única. Acesso vitalício ao produto e todas as suas atualizações.
              </p>
            </div>

            {/* CTAs */}
            <div className="mt-5 space-y-3">
              <button
                type="button"
                onClick={() => {
                  document.getElementById('purchase-options')?.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start',
                  });
                }}
                className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary-500 py-3 font-semibold text-white transition hover:bg-primary-600"
              >
                <Download className="h-4 w-4" />
                Comprar agora
              </button>

              <button
                type="button"
                onClick={() => {
                  document.getElementById('purchase-options')?.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start',
                  });
                }}
                className="inline-flex w-full items-center justify-center gap-2 rounded-xl border border-neutral-300 bg-white py-3 font-semibold text-brand-black transition hover:bg-neutral-100"
              >
                <ShoppingCart className="h-4 w-4" />
                Selecionar plantas
              </button>
            </div>

            {/* trust row */}
            <div className="mt-4 flex flex-wrap items-center gap-6 text-xs font-semibold text-brand-muted">
              <span className="inline-flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary-500" />
                Pagamento Seguro
              </span>
              <span className="inline-flex items-center gap-2">
                <Headset className="h-4 w-4 text-primary-500" />
                Suporte Profissional
              </span>
              <span className="inline-flex items-center gap-2">
                <BadgeCheck className="h-4 w-4 text-primary-500" />
                Certificação CAU
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

/* ---------- helpers ---------- */

function BadgePill({ children, tone }: { children: React.ReactNode; tone: 'orange' | 'green' }) {
  const cls =
    tone === 'orange'
      ? 'bg-orange-50 text-primary-700 border-orange-100'
      : 'bg-green-50 text-brand-green border-green-100';

  return (
    <span className={`px-3 py-1 rounded-full text-[11px] font-extrabold border ${cls}`}>
      {children}
    </span>
  );
}

function InfoMini({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex flex-col items-center text-center gap-1">
      <div className="h-10 w-10 rounded-xl bg-orange-50 border border-orange-100 flex items-center justify-center">
        {icon}
      </div>
      <div className="text-xs font-extrabold text-brand-muted">{label}</div>
      <div className="text-sm font-extrabold text-brand-black">{value}</div>
    </div>
  );
}

function formatFormats(formats: string[]) {
  return formats.length > 0 ? formats.join('/') : '—';
}

