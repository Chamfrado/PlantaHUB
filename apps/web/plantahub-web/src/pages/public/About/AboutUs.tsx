// src/pages/AboutPage.tsx
import {
  ArrowRight,
  BadgeCheck,
  BadgeDollarSign,
  Building2,
  CheckCircle2,
  Clock3,
  Cpu,
  Leaf,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import { sectionByKey, useSitePage } from '../../../hooks/useSitePage';


/**
 * Os icones fazem parte do desenho da pagina, nao do texto que o administrador edita, e por
 * isso continuam no codigo — casados por posicao com os itens da secao. Item a mais do que
 * icones simplesmente nao recebe icone, em vez de quebrar a tela.
 */
const MANIFESTO_ICONS = [
  <Sparkles className="h-5 w-5 text-primary-600" key="sparkles" />,
  <Clock3 className="h-5 w-5 text-primary-600" key="clock" />,
  <BadgeDollarSign className="h-5 w-5 text-primary-600" key="money" />,
];

const IDENTITY_ICONS = [
  <Sparkles className="h-5 w-5 text-primary-600" key="sparkles" />,
  <Cpu className="h-5 w-5 text-primary-600" key="cpu" />,
  <CheckCircle2 className="h-5 w-5 text-primary-600" key="check" />,
];

const PILLAR_ICONS = [
  <Building2 className="h-5 w-5 text-white" key="building" />,
  <Cpu className="h-5 w-5 text-white" key="cpu" />,
  <Leaf className="h-5 w-5 text-white" key="leaf" />,
];

const COMPLIANCE_ICONS = [
  <BadgeCheck className="h-5 w-5 text-primary-400" key="badge" />,
  <ShieldCheck className="h-5 w-5 text-primary-400" key="shield" />,
  <CheckCircle2 className="h-5 w-5 text-primary-400" key="check" />,
];

export default function AboutUs() {
  const { content } = useSitePage('sobre');
  return (
    <div className="bg-white">
      {/* HERO */}
      <section className="bg-white">
        <div className="max-w-7xl mx-auto px-6 py-16 md:py-20">
          <div className="grid gap-10 lg:grid-cols-2 lg:items-center">
            <div>
              <span className="inline-flex items-center rounded-full bg-white text-primary-700 border border-orange-100 px-3 py-1 text-xs font-extrabold tracking-wide">{content.subheadline}</span>

              <h1 className="mt-4 text-4xl md:text-5xl font-extrabold text-brand-black leading-tight">{content.headline}</h1>

              <p className="mt-4 text-brand-muted leading-relaxed max-w-xl">{content.intro}</p>
            </div>

            {/* image card (use your own image path in public/) */}
            <div className="lg:justify-self-end">
              <div className="relative">
                <div className="absolute -top-4 -right-4 h-14 w-14 rounded-xl border-2 border-primary-500" />
                <div className="absolute -bottom-5 -left-6 h-20 w-20 rounded-2xl bg-orange-100/70" />

                <div className="rounded-2xl border border-neutral-200 overflow-hidden shadow-sm bg-brand-light">
                  <img
                    src="/brand/logo-primary.png"
                    alt="Equipe PlantaHUB"
                    className="w-full h-80 md:h-80 object-contain bg-white p-10"
                    loading="lazy"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* MANIFESTO */}
      <section className="bg-white">
        <div className="max-w-7xl mx-auto px-6 py-16">
          <div className="text-center">
            <h2 className="text-2xl md:text-3xl font-extrabold text-brand-black">{sectionByKey(content, 'manifesto')?.title}</h2>
            <div className="mt-3 mx-auto h-1 w-10 rounded-full bg-primary-500" />
          </div>

          <div className="mt-10 max-w-4xl mx-auto rounded-2xl border border-neutral-200 bg-white p-8">
            <div className="grid gap-6 md:grid-cols-2">
              <p className="text-brand-muted leading-relaxed">
                Acreditamos que a arquitetura deve ser{' '}
                <span className="font-bold text-brand-black">
                  acessível, inteligente e responsável
                </span>
                .
              </p>
              <p className="text-brand-muted leading-relaxed">{sectionByKey(content, 'manifesto')?.body}</p>
            </div>
          </div>

          <div className="mt-10 grid gap-6 md:grid-cols-3 max-w-4xl mx-auto">
            {(sectionByKey(content, 'manifesto')?.items ?? []).map((value, i) => (
              <MiniValue
                key={value.title}
                icon={MANIFESTO_ICONS[i] ?? null}
                title={value.title ?? ''}
                text={value.text ?? ''}
              />
            ))}
          </div>

          <div className="mt-10 max-w-4xl mx-auto rounded-2xl bg-brand-graphite text-white p-8">
            <p className="text-sm text-neutral-200 leading-relaxed">{sectionByKey(content, 'proposito')?.body}</p>
            <p className="mt-3 text-sm text-neutral-200 leading-relaxed">
              Ao unir construção, tecnologia e sustentabilidade, possibilitamos que pessoas
              construam com confiança — e que profissionais ampliem seu impacto.
            </p>
          </div>

          <div className="mt-10 max-w-4xl mx-auto grid gap-6 md:grid-cols-3 text-center">
            {(sectionByKey(content, 'proposito')?.items ?? []).map(metric => (
              <FooterMetric key={metric.title} title={metric.title ?? ''} subtitle={metric.text ?? ''} />
            ))}
          </div>

          <div className="mt-10 text-center max-w-4xl mx-auto">
            <p className="text-brand-muted">
              Projetamos não apenas para as necessidades de hoje, mas para um ambiente construído
              mais eficiente, sustentável e inteligente.
            </p>
            <p className="mt-2 font-extrabold text-brand-black">
              Isso é arquitetura impulsionada por tecnologia.
              <span className="text-primary-600"> Isso é PLANTAHUB.</span>
            </p>
          </div>
        </div>
      </section>

      {/* MISSION / VISION / VALUES */}
      <section className="bg-brand-light">
        <div className="max-w-7xl mx-auto px-6 py-16">
          <div className="grid gap-6 md:grid-cols-3">
            {(sectionByKey(content, 'identidade')?.items ?? []).map((card, i) => (
              <InfoCard
                key={card.title}
                icon={IDENTITY_ICONS[i] ?? null}
                title={card.title ?? ''}
                text={card.text ?? ''}
                list={card.bullets}
              />
            ))}
          </div>

          <div className="mt-16 text-center">
            <h2 className="text-2xl md:text-3xl font-extrabold text-brand-black">{sectionByKey(content, 'pilares')?.title}</h2>
            <p className="mt-2 text-brand-muted">{sectionByKey(content, 'pilares')?.body}</p>
          </div>

          <div className="mt-10 grid gap-6 md:grid-cols-3">
            {(sectionByKey(content, 'pilares')?.items ?? []).map((pillar, i) => (
              <PillarCard
                key={pillar.title}
                icon={PILLAR_ICONS[i] ?? null}
                title={pillar.title ?? ''}
                text={pillar.text ?? ''}
                bullets={pillar.bullets}
              />
            ))}
          </div>
        </div>
      </section>

      {/* COMPLIANCE + STATS */}
      <section className="bg-brand-graphite text-white">
        <div className="max-w-7xl mx-auto px-6 py-16">
          <div className="grid gap-10 lg:grid-cols-2 lg:items-center">
            <div>
              <h2 className="text-2xl md:text-3xl font-extrabold">{sectionByKey(content, 'conformidade')?.title}</h2>
              <p className="mt-3 text-neutral-200 leading-relaxed max-w-xl">{sectionByKey(content, 'conformidade')?.body}</p>

              <div className="mt-8 space-y-4">
                {(sectionByKey(content, 'conformidade')?.items ?? []).map((entry, i) => (
                  <ComplianceItem
                    key={entry.title}
                    icon={COMPLIANCE_ICONS[i] ?? null}
                    title={entry.title ?? ''}
                    text={entry.text ?? ''}
                  />
                ))}
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <DarkStat value="100%" label="Plantas certificadas" />
              <DarkStat value="50+" label="Arquitetos" />
              <DarkStat value="500+" label="Projetos entregues" />
              <DarkStat value="24/7" label="Suporte" />
            </div>
          </div>
        </div>
      </section>

      {/* TEAM */}
      <section className="bg-white">
        <div className="max-w-7xl mx-auto px-6 py-16">
          <div className="text-center">
            <h2 className="text-2xl md:text-3xl font-extrabold text-brand-black">{sectionByKey(content, 'time')?.title}</h2>
            <p className="mt-2 text-brand-muted max-w-3xl mx-auto">{sectionByKey(content, 'time')?.body}</p>
          </div>

          <div className="mt-10 grid gap-8 sm:grid-cols-2 lg:grid-cols-2">
            <TeamMember
              name="Alberto Baldini Kersul"
              role="CEO"
              cert="Administração"
              avatar="/photos/alberto.jpg"
            />
            <TeamMember
              name="Lohran Cintra"
              role="CTO"
              cert="Sistemas de Informação"
              avatar="/photos/lohran.jpg"
            />
          </div>

          <div className="mt-12 max-w-4xl mx-auto rounded-2xl border border-neutral-200 bg-white p-8 text-center">
            <p className="text-brand-muted leading-relaxed">
              Combinamos décadas de experiência em arquitetura com tecnologia de ponta para entregar
              soluções tecnicamente excelentes e profundamente responsáveis às necessidades humanas
              e ambientais.
            </p>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="bg-primary-500">
        <div className="max-w-7xl mx-auto px-6 py-16 text-center">
          <h2 className="text-3xl font-extrabold text-white">{content.ctaTitle}</h2>
          <p className="mt-2 text-white/90">{content.ctaSubtitle}</p>

          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <button className="rounded-xl bg-white text-brand-black font-semibold px-6 py-3 hover:bg-neutral-100 transition inline-flex items-center gap-2">
              Explorar Plantas <ArrowRight className="h-4 w-4" />
            </button>
            <button className="rounded-xl border border-white/40 text-white font-semibold px-6 py-3 hover:bg-white/10 transition">
              Fale Conosco
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

/* ---------- components ---------- */

function MiniValue({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return (
    <div className="text-center">
      <div className="mx-auto h-12 w-12 rounded-2xl bg-orange-50 border border-orange-100 flex items-center justify-center">
        {icon}
      </div>
      <div className="mt-3 font-extrabold text-brand-black">{title}</div>
      <div className="mt-1 text-sm text-brand-muted">{text}</div>
    </div>
  );
}

function FooterMetric({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div>
      <div className="font-extrabold text-brand-black">{title}</div>
      <div className="mt-1 text-sm text-brand-muted">{subtitle}</div>
    </div>
  );
}

function InfoCard({
  icon,
  title,
  text,
  list,
}: {
  icon: React.ReactNode;
  title: string;
  text?: string;
  list?: string[];
}) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
      <div className="h-10 w-10 rounded-xl bg-orange-50 border border-orange-100 flex items-center justify-center">
        {icon}
      </div>
      <div className="mt-4 font-extrabold text-brand-black">{title}</div>
      {text ? <p className="mt-2 text-sm text-brand-muted leading-relaxed">{text}</p> : null}
      {list?.length ? (
        <ul className="mt-3 space-y-2 text-sm text-brand-muted">
          {list.map(x => (
            <li key={x} className="flex items-center gap-2">
              <span className="text-primary-500">✓</span>
              <span>{x}</span>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

function PillarCard({
  icon,
  title,
  text,
  bullets,
}: {
  icon: React.ReactNode;
  title: string;
  text: string;
  bullets: string[];
}) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
      <div className="h-12 w-12 rounded-2xl bg-primary-500 flex items-center justify-center">
        {icon}
      </div>
      <div className="mt-4 font-extrabold text-brand-black">{title}</div>
      <p className="mt-2 text-sm text-brand-muted leading-relaxed">{text}</p>
      <ul className="mt-4 space-y-2 text-sm text-brand-muted">
        {bullets.map(b => (
          <li key={b} className="flex items-center gap-2">
            <span className="h-1.5 w-1.5 rounded-full bg-primary-500" />
            <span>{b}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

function ComplianceItem({
  icon,
  title,
  text,
}: {
  icon: React.ReactNode;
  title: string;
  text: string;
}) {
  return (
    <div className="flex items-start gap-3">
      <div className="mt-0.5 h-9 w-9 rounded-xl bg-white/10 border border-white/10 flex items-center justify-center">
        {icon}
      </div>
      <div>
        <div className="font-extrabold">{title}</div>
        <div className="text-sm text-neutral-200">{text}</div>
      </div>
    </div>
  );
}

function DarkStat({ value, label }: { value: string; label: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 p-6">
      <div className="text-3xl font-extrabold text-primary-500">{value}</div>
      <div className="mt-1 text-sm font-semibold text-neutral-200">{label}</div>
    </div>
  );
}

function TeamMember({
  name,
  role,
  cert,
  avatar,
}: {
  name: string;
  role: string;
  cert: string;
  avatar: string;
}) {
  return (
    <div className="text-center">
      <div className="mx-auto h-20 w-20 rounded-full overflow-hidden bg-neutral-200">
        <img src={avatar} alt={name} className="h-full w-full object-cover" loading="lazy" />
      </div>
      <div className="mt-3 font-extrabold text-brand-black">{name}</div>
      <div className="mt-1 text-sm text-brand-muted">{role}</div>
      <div className="mt-1 text-xs font-semibold text-brand-muted">{cert}</div>
    </div>
  );
}
