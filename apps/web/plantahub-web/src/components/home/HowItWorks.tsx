import { BadgeCheck, Headset, Lock } from 'lucide-react';
import React from 'react';

type Step = {
  number: number;
  title: string;
  description: string;
};

type TrustItem = {
  title: string;
  description: string;
  icon: React.ReactNode;
};

type Stat = {
  value: string;
  label: string;
};

const steps: Step[] = [
  {
    number: 1,
    title: 'Defina seu terreno e necessidade',
    description: ' Escolha o tipo de construção ideal para seu espaço e objetivo.',
  },
  {
    number: 2,
    title: 'Explore os Projetos',
    description: 'Navegue pelo catálogo com opções organizadas por categoria, estilo e metragem.',
  },
  {
    number: 3,
    title: ' Escolha o projeto ideal',
    description: 'Visualize detalhes, documentos inclusos e prévias antes de tomar sua decisão.',
  },
  {
    number: 4,
    title: 'Pagamento Seguro',
    description: 'Finalize a compra com checkout criptografado e confirmação imediata do pedido.',
  },
  {
    number: 5,
    title: 'Download Imediato',
    description: 'Receba seu projeto na hora e comece sua obra sem burocracia.',
  },
];

const trustItems: TrustItem[] = [
  {
    title: 'Arquitetos e Engenheiros Habilitados',
    description:
      'Projetos desenvolvidos por profissionais habilitados, com registro nos órgãos competentes e seguindo rigorosamente as normas técnicas vigentes. Trazendo desgin inteligente e soluções funcionais.',
    icon: <BadgeCheck className="h-7 w-7" />,
  },
  {
    title: 'Pagamentos 100% Seguros',
    description:
      'Ambiente protegido com criptografia de nível bancário, garantindo total segurança em todas as transações.',
    icon: <Lock className="h-7 w-7" />,
  },
  {
    title: 'Suporte Humanizado',
    description:
      'Conte com uma equipe preparada para esclarecer dúvidas e oferecer orientação sempre que necessário.',
    icon: <Headset className="h-7 w-7" />,
  },
];

const stats: Stat[] = [
  { value: '500+', label: 'Projetos disponíveis' },
  { value: '10K+', label: 'Clientes satisfeitos' },
  { value: '98%', label: 'Taxa de satisfação' },
  { value: '24/7', label: 'Suporte disponível' },
];

function StepItem({ step }: { step: Step }) {
  return (
    <div className="text-center">
      <div className="mx-auto h-12 w-12 rounded-full bg-orange-50 flex items-center justify-center">
        <span className="font-extrabold text-primary-500">{step.number}</span>
      </div>

      <h4 className="mt-5 font-bold text-neutral-900">{step.title}</h4>

      <p className="mt-3 text-sm leading-relaxed text-neutral-600">{step.description}</p>
    </div>
  );
}

function TrustCard({ item }: { item: TrustItem }) {
  return (
    <div className="text-center">
      <div className="mx-auto h-14 w-14 rounded-2xl bg-orange-50 flex items-center justify-center text-2xl text-primary-500">
        {item.icon}
      </div>

      <h4 className="mt-6 font-bold text-neutral-900">{item.title}</h4>

      <p className="mt-3 text-sm leading-relaxed text-neutral-600">{item.description}</p>
    </div>
  );
}

export default function HowItWorks() {
  return (
    <section className="bg-white">
      <div className="max-w-7xl mx-auto px-6 py-16 md:py-20">
        {/* HOW IT WORKS */}
        <div className="text-center  max-w-3xl mx-auto">
          <h2 className="text-3xl md:text-4xl font-extrabold text-neutral-900">Como funciona</h2>
          <p className="mt-3 text-neutral-600">Processo de compra simples, rápido e seguro</p>
        </div>

        <div className="mt-12 grid bg-neutral-50 border-neutral-100 rounded-2xl gap-10 md:grid-cols-5">
          {steps.map(s => (
            <StepItem key={s.number} step={s} />
          ))}
        </div>

        {/* Spacer */}
        <div className="mt-16 md:mt-20" />

        {/* TRUST */}
        <div className="text-center max-w-3xl mx-auto">
          <h3 className="text-2xl md:text-3xl font-extrabold text-neutral-900">
            Qualidade profissional em que você pode confiar
          </h3>
          <p className="mt-3 text-neutral-600">
            Segurança, credibilidade e suporte em cada etapa da sua jornada.
          </p>
        </div>

        <div className="mt-12 grid gap-8 md:grid-cols-3">
          {trustItems.map((t, idx) => (
            <TrustCard key={idx} item={t} />
          ))}
        </div>

        {/* Stats bar */}
        <div className="mt-14 bg-neutral-50 border border-neutral-100 rounded-2xl px-6 py-8">
          <div className="grid gap-8 sm:grid-cols-2 md:grid-cols-4">
            {stats.map((st, idx) => (
              <div key={idx} className="text-center">
                <div className="text-2xl font-extrabold text-primary-500">{st.value}</div>
                <div className="mt-1 text-sm text-neutral-600">{st.label}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
