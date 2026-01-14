type Props = {
  title?: string;
  subtitle?: string;
  primaryLabel?: string;
  secondaryLabel?: string;
  onPrimaryClick?: () => void;
  onSecondaryClick?: () => void;
  bullets?: string[];
};

export default function FinalCTA({
  title = 'Comece a construir seu sonho hoje',
  subtitle = 'Junte-se a milhares de clientes satisfeitos que construíram com sucesso usando nossas plantas arquitetônicas profissionais.',
  primaryLabel = 'Explorar todos os projetos',
  secondaryLabel = 'Falar com o Comercial',
  onPrimaryClick,
  onSecondaryClick,
  bullets = ['Download imediato', 'Atualizações vitalícias', 'Suporte especializado'],
}: Props) {
  return (
    <section className="bg-primary-500">
      <div className="max-w-7xl mx-auto px-6 py-16 md:py-20 text-center">
        <h2 className="text-3xl md:text-4xl font-extrabold text-white">{title}</h2>

        <p className="mt-4 text-white/90 max-w-2xl mx-auto">{subtitle}</p>

        <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
          <button
            onClick={onPrimaryClick}
            className="px-7 py-3 rounded-xl bg-white text-primary-600 font-semibold shadow-sm hover:bg-white/95 transition"
          >
            {primaryLabel}
          </button>

          <button
            onClick={onSecondaryClick}
            className="px-7 py-3 rounded-xl border border-white/70 text-white font-semibold hover:bg-white/10 transition"
          >
            {secondaryLabel}
          </button>
        </div>

        <div className="mt-10 flex flex-wrap items-center justify-center gap-x-10 gap-y-3 text-white/95 text-sm font-semibold">
          {bullets.map(b => (
            <div key={b} className="inline-flex items-center gap-2">
              <span className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-white text-primary-600 text-xs">
                ✓
              </span>
              <span>{b}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
