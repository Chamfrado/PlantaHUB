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

export default function AboutUs() {
  return (
    <div className="bg-white">
      {/* HERO */}
      <section className="bg-white">
        <div className="max-w-7xl mx-auto px-6 py-16 md:py-20">
          <div className="grid gap-10 lg:grid-cols-2 lg:items-center">
            <div>
              <span className="inline-flex items-center rounded-full bg-orange-50 text-primary-700 border border-orange-100 px-3 py-1 text-xs font-extrabold tracking-wide">
                SOBRE A PLANTAHUB
              </span>

              <h1 className="mt-4 text-4xl md:text-5xl font-extrabold text-neutral-900 leading-tight">
                Arquitetura impulsionada por tecnologia
              </h1>

              <p className="mt-4 text-neutral-600 leading-relaxed max-w-xl">
                Transformamos conhecimento arquitetônico em soluções digitais escaláveis para
                conectar profissionais e impulsionar uma construção mais inteligente, eficiente e
                sustentável.
              </p>
            </div>

            {/* image card (use your own image path in public/) */}
            <div className="lg:justify-self-end">
              <div className="relative">
                <div className="absolute -top-4 -right-4 h-14 w-14 rounded-xl border-2 border-primary-500" />
                <div className="absolute -bottom-5 -left-6 h-20 w-20 rounded-2xl bg-orange-100/70" />

                <div className="rounded-2xl border border-neutral-200 overflow-hidden shadow-sm bg-neutral-50">
                  <img
                    src="/brand/logotipo.png"
                    alt="Equipe PlantaHUB"
                    className="w-full h-80 md:h-80 object-cover"
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
            <h2 className="text-2xl md:text-3xl font-extrabold text-neutral-900">
              Manifesto da Marca
            </h2>
            <div className="mt-3 mx-auto h-1 w-10 rounded-full bg-primary-500" />
          </div>

          <div className="mt-10 max-w-4xl mx-auto rounded-2xl border border-neutral-200 bg-white p-8">
            <div className="grid gap-6 md:grid-cols-2">
              <p className="text-neutral-600 leading-relaxed">
                Acreditamos que a arquitetura deve ser{' '}
                <span className="font-bold text-neutral-900">
                  acessível, inteligente e responsável
                </span>
                .
              </p>
              <p className="text-neutral-600 leading-relaxed">
                Acreditamos que construir não precisa ser lento, caro ou desconectado da tecnologia.
              </p>
            </div>
          </div>

          <div className="mt-10 grid gap-6 md:grid-cols-3 max-w-4xl mx-auto">
            <MiniValue
              icon={<Sparkles className="h-5 w-5 text-primary-600" />}
              title="Simplificar a complexidade"
              text="Transformamos documentação técnica em uma experiência clara e guiada."
            />
            <MiniValue
              icon={<Clock3 className="h-5 w-5 text-primary-600" />}
              title="Reduzir tempo"
              text="Aceleramos o ciclo do projeto com entrega digital e organização profissional."
            />
            <MiniValue
              icon={<BadgeDollarSign className="h-5 w-5 text-primary-600" />}
              title="Otimizar custo"
              text="Menos retrabalho, mais previsibilidade e decisões melhores desde o início."
            />
          </div>

          <div className="mt-10 max-w-4xl mx-auto rounded-2xl bg-neutral-900 text-white p-8">
            <p className="text-sm text-neutral-200 leading-relaxed">
              Existimos para transformar conhecimento arquitetônico em soluções digitais escaláveis,
              conectando arquitetos, engenheiros e construtores por meio de um marketplace simples,
              confiável e pronto para obra.
            </p>
            <p className="mt-3 text-sm text-neutral-200 leading-relaxed">
              Ao unir construção, tecnologia e sustentabilidade, possibilitamos que pessoas
              construam com confiança — e que profissionais ampliem seu impacto.
            </p>
          </div>

          <div className="mt-10 max-w-4xl mx-auto grid gap-6 md:grid-cols-3 text-center">
            <FooterMetric title="Excelência Técnica" subtitle="Precisão em cada detalhe" />
            <FooterMetric title="Conformidade Legal" subtitle="CAU / CREA" />
            <FooterMetric title="Responsabilidade Ecológica" subtitle="Sustentável por design" />
          </div>

          <div className="mt-10 text-center max-w-4xl mx-auto">
            <p className="text-neutral-600">
              Projetamos não apenas para as necessidades de hoje, mas para um ambiente construído
              mais eficiente, sustentável e inteligente.
            </p>
            <p className="mt-2 font-extrabold text-neutral-900">
              Isso é arquitetura impulsionada por tecnologia.
              <span className="text-primary-600"> Isso é PLANTAHUB.</span>
            </p>
          </div>
        </div>
      </section>

      {/* MISSION / VISION / VALUES */}
      <section className="bg-neutral-50">
        <div className="max-w-7xl mx-auto px-6 py-16">
          <div className="grid gap-6 md:grid-cols-3">
            <InfoCard
              icon={<Sparkles className="h-5 w-5 text-primary-600" />}
              title="Missão"
              text="Transformar conhecimento arquitetônico em soluções digitais acessíveis, reduzindo tempo e custo, mantendo excelência técnica e conformidade legal em cada projeto."
            />
            <InfoCard
              icon={<Cpu className="h-5 w-5 text-primary-600" />}
              title="Visão"
              text="Ser a principal infraestrutura digital para profissionais de arquitetura e construção, criando um ambiente construído mais eficiente e sustentável em escala."
            />
            <InfoCard
              icon={<CheckCircle2 className="h-5 w-5 text-primary-600" />}
              title="Valores"
              list={[
                'Precisão técnica',
                'Inovação acessível',
                'Práticas sustentáveis',
                'Confiança profissional',
              ]}
            />
          </div>

          <div className="mt-16 text-center">
            <h2 className="text-2xl md:text-3xl font-extrabold text-neutral-900">
              Três Pilares de Integração
            </h2>
            <p className="mt-2 text-neutral-600">
              Onde construção, tecnologia e sustentabilidade convergem
            </p>
          </div>

          <div className="mt-10 grid gap-6 md:grid-cols-3">
            <PillarCard
              icon={<Building2 className="h-5 w-5 text-white" />}
              title="Construção"
              text="Plantas arquitetônicas em padrão profissional, compatíveis com CAU/CREA, garantindo conformidade e excelência."
              bullets={[
                'Profissionais certificados',
                'Conformidade regulatória',
                'Precisão técnica',
              ]}
            />
            <PillarCard
              icon={<Cpu className="h-5 w-5 text-white" />}
              title="Tecnologia"
              text="Infraestrutura digital que escala conhecimento e distribui projetos de forma instantânea."
              bullets={['Entrega imediata', 'Marketplace digital', 'Plataforma escalável']}
            />
            <PillarCard
              icon={<Leaf className="h-5 w-5 text-white" />}
              title="Sustentabilidade"
              text="Responsabilidade ecológica incorporada em princípios de design, otimizando recursos e reduzindo impacto."
              bullets={['Otimização de recursos', 'Design eco-consciente', 'Menos desperdício']}
            />
          </div>
        </div>
      </section>

      {/* COMPLIANCE + STATS */}
      <section className="bg-neutral-900 text-white">
        <div className="max-w-7xl mx-auto px-6 py-16">
          <div className="grid gap-10 lg:grid-cols-2 lg:items-center">
            <div>
              <h2 className="text-2xl md:text-3xl font-extrabold">
                Conformidade Profissional & Certificação
              </h2>
              <p className="mt-3 text-neutral-200 leading-relaxed max-w-xl">
                Cada planta na PLANTAHUB é pensada para atender padrões profissionais e requisitos
                regulatórios, com documentação completa para obra e aprovação.
              </p>

              <div className="mt-8 space-y-4">
                <ComplianceItem
                  icon={<BadgeCheck className="h-5 w-5 text-primary-500" />}
                  title="Certificação CAU"
                  text="Projetos preparados para padrões de Arquitetura e Urbanismo."
                />
                <ComplianceItem
                  icon={<ShieldCheck className="h-5 w-5 text-primary-500" />}
                  title="Aprovado para conformidade"
                  text="Documentação pronta para apoiar processos e validações."
                />
                <ComplianceItem
                  icon={<CheckCircle2 className="h-5 w-5 text-primary-500" />}
                  title="Conformidade legal"
                  text="Documentação regulatória incluída conforme o pacote."
                />
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
            <h2 className="text-2xl md:text-3xl font-extrabold text-neutral-900">
              Humanizado. Profissional. Confiável.
            </h2>
            <p className="mt-2 text-neutral-600 max-w-3xl mx-auto">
              Por trás de cada planta existe um time de profissionais experientes comprometidos em
              transformar sua visão em realidade com precisão e cuidado.
            </p>
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
            <p className="text-neutral-600 leading-relaxed">
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
          <h2 className="text-3xl font-extrabold text-white">
            Pronto para construir com confiança?
          </h2>
          <p className="mt-2 text-white/90">
            Junte-se a milhares de profissionais e clientes que confiam na PLANTAHUB.
          </p>

          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <button className="rounded-xl bg-white text-neutral-900 font-semibold px-6 py-3 hover:bg-neutral-100 transition inline-flex items-center gap-2">
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
      <div className="mt-3 font-extrabold text-neutral-900">{title}</div>
      <div className="mt-1 text-sm text-neutral-600">{text}</div>
    </div>
  );
}

function FooterMetric({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div>
      <div className="font-extrabold text-neutral-900">{title}</div>
      <div className="mt-1 text-sm text-neutral-600">{subtitle}</div>
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
      <div className="mt-4 font-extrabold text-neutral-900">{title}</div>
      {text ? <p className="mt-2 text-sm text-neutral-600 leading-relaxed">{text}</p> : null}
      {list?.length ? (
        <ul className="mt-3 space-y-2 text-sm text-neutral-700">
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
      <div className="mt-4 font-extrabold text-neutral-900">{title}</div>
      <p className="mt-2 text-sm text-neutral-600 leading-relaxed">{text}</p>
      <ul className="mt-4 space-y-2 text-sm text-neutral-700">
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
      <div className="mt-3 font-extrabold text-neutral-900">{name}</div>
      <div className="mt-1 text-sm text-neutral-600">{role}</div>
      <div className="mt-1 text-xs font-semibold text-neutral-500">{cert}</div>
    </div>
  );
}
