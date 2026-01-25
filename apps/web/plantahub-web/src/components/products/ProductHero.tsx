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
import type { Product } from '../../types/ProductData';

type Props = {
  product: Product;
};

export default function ProductHero({ product }: Props) {
  const title = product.page.headline ?? product.name;
  const description = product.page.description ?? '';
  const subtitle = product.page.subheadline ?? product.shortDescription ?? '';

  const gallery = useMemo(() => {
    const list = [product.heroImageUrl, ...(product.galleryImageUrls ?? [])].filter(
      Boolean
    ) as string[];
    // avoid duplicates
    return Array.from(new Set(list));
  }, [product.heroImageUrl, product.galleryImageUrls]);

  const [activeImg, setActiveImg] = useState<string | undefined>(gallery[0]);

  const price = product.price ? formatMoney(product.price.amount, product.price.currency) : null;

  return (
    <section className="bg-white">
      <div className="max-w-7xl mx-auto px-6 pt-8 pb-10">
        {/* Breadcrumb */}
        <div className="text-sm text-neutral-500">
          <Link className="hover:text-neutral-900 cursor-pointer" to="/">
            Home
          </Link>
          <span className="mx-2">/</span>
          <Link className="hover:text-neutral-900" to={`/products`}>
            {labelCategory(product.category)}
          </Link>
          <span className="mx-2">/</span>
          <span className="text-neutral-800 font-semibold">{title}</span>
        </div>

        <div className="mt-6 grid gap-8 lg:grid-cols-2 lg:items-start">
          {/* LEFT — Gallery */}
          <div>
            <div className="rounded-2xl border border-neutral-200 overflow-hidden bg-neutral-50">
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
            {gallery.length > 1 ? (
              <div className="mt-4 flex gap-3">
                {gallery.slice(0, 5).map(src => {
                  const active = src === activeImg;
                  return (
                    <button
                      key={src}
                      onClick={() => setActiveImg(src)}
                      className={[
                        'h-16 w-24 rounded-xl overflow-hidden border transition',
                        active
                          ? 'border-primary-500 ring-2 ring-primary-200'
                          : 'border-neutral-200 hover:border-neutral-300',
                      ].join(' ')}
                      aria-label="Selecionar imagem"
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
            <div className="flex flex-wrap gap-2">
              <BadgePill tone="orange">MAIS COMPRADO</BadgePill>
              <BadgePill tone="green">ECOLÓGICO</BadgePill>
            </div>

            <h1 className="mt-3 text-4xl font-extrabold text-neutral-900">{title}</h1>

            {subtitle ? <p className="mt-2 text-neutral-600 font-semibold">{subtitle}</p> : null}

            {description ? (
              <p className="mt-3 text-neutral-600 leading-relaxed">{description}</p>
            ) : null}

            {/* quick stats row */}
            <div className="mt-6 grid grid-cols-2 sm:grid-cols-4 gap-4 border-y border-neutral-200 py-5">
              <InfoMini
                icon={<Ruler className="h-5 w-5 text-primary-500" />}
                label="Area"
                value={typeof product.areaM2 === 'number' ? `${product.areaM2} m²` : '—'}
              />
              <InfoMini
                icon={<FileText className="h-5 w-5 text-primary-500" />}
                label="Formatos"
                value={formatFormats(product.fileFormats)}
              />
              <InfoMini
                icon={<Zap className="h-5 w-5 text-primary-500" />}
                label="Entrega"
                value={product.delivery ? 'Instant' : '—'}
              />
              <InfoMini
                icon={<SlidersHorizontal className="h-5 w-5 text-primary-500" />}
                label="Customizavel"
                value={product.customizable ? 'Yes' : 'No'}
              />
            </div>

            {/* price */}
            <div className="mt-5">
              {price ? (
                <div className="flex items-end gap-2">
                  <div className="text-4xl font-extrabold text-neutral-900">{price}</div>
                  <div className="pb-1 text-sm font-semibold text-neutral-500">
                    {product.price?.currency ?? ''}
                  </div>
                </div>
              ) : (
                <div className="text-xl font-extrabold text-neutral-900">Consulte valores</div>
              )}

              <p className="mt-2 text-sm text-neutral-600">
                Compra única. Acesso vitalício ao produto e todas as suas atualizações.
              </p>
            </div>

            {/* CTAs */}
            <div className="mt-5 space-y-3">
              <button className="w-full rounded-xl bg-primary-500 text-white font-semibold py-3 hover:bg-primary-600 transition inline-flex items-center justify-center gap-2">
                <Download className="h-4 w-4" />
                Compre Agora - Download Instantâneo
              </button>

              <button className="w-full rounded-xl border border-neutral-300 bg-white text-neutral-900 font-semibold py-3 hover:bg-neutral-100 transition inline-flex items-center justify-center gap-2">
                <ShoppingCart className="h-4 w-4" />
                Adicionar ao Carrinho
              </button>
            </div>

            {/* trust row */}
            <div className="mt-4 flex flex-wrap items-center gap-6 text-xs font-semibold text-neutral-600">
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
      : 'bg-emerald-50 text-emerald-700 border-emerald-100';

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
      <div className="text-xs font-extrabold text-neutral-700">{label}</div>
      <div className="text-sm font-extrabold text-neutral-900">{value}</div>
    </div>
  );
}

function formatFormats(formats?: string[]) {
  if (!formats?.length) return '—';
  // your screenshot shows BIM/DWG (and maybe PDF hidden). you can customize:
  return formats.join('/');
}

function labelCategory(category: Product['category']) {
  if (category === 'casas') return 'Houses';
  if (category === 'chales') return 'Chalets';
  return 'Studios';
}

function formatMoney(value: number, currency: 'BRL' | 'USD' | 'EUR') {
  const locale = currency === 'BRL' ? 'pt-BR' : 'en-US';
  return value.toLocaleString(locale, {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  });
}
