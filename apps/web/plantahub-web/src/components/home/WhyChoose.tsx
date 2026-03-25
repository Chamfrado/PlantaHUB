import { BrickWall, CheckCheck, DollarSign, StickyNote } from 'lucide-react';
import React from 'react';

type Benefit = {
  title: string;
  description: string;
  icon: React.ReactNode;
};

const benefits: Benefit[] = [
  {
    title: 'Projetos Completos',
    description:
      'Desenvolvidos com integração entre todos os sistemas da obra, garantindo mais eficiência, compatibilidade e facilidade na execução.',
    icon: <BrickWall className="h-7 w-7" />,
  },
  {
    title: 'Documentação Completa',
    description:
      'Plantas arquitetônicas, estruturais, elétricas, hidráulicas, paisagistica, material de apoio e renderização incluídas.',
    icon: <StickyNote className="h-7 w-7" />,
  },
  {
    title: 'Projetos Aprovados',
    description:
      'Todos os projetos acompanham ART e RRT, garantindo responsabilidade técnica e conformidade com as exigências legais.',
    icon: <CheckCheck className="h-7 w-7" />,
  },
  {
    title: 'Redução Inteligente de Custo e Tempo',
    description:
      'Economize até 60% em comparação com projetos personalizados, sem abrir mão da qualidade.\n  Projetos otimizados para aprovação e execução.',
    icon: <DollarSign className="h-7 w-7" />,
  },
];

export default function WhyChoose() {
  return (
    <section className="bg-white">
      <div className="max-w-7xl mx-auto px-6 py-16 md:py-20">
        {/* Title */}
        <div className="text-center max-w-3xl mx-auto">
          <h2 className="text-3xl md:text-4xl font-extrabold text-neutral-900">
            Por que escolher nossas plantas arquitetônicas
          </h2>
          <p className="mt-4 text-neutral-600 text-base md:text-lg">
            Completas, profissionais e prontas para construir
          </p>
        </div>

        {/* Cards */}
        <div className="mt-12 grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          {benefits.map((b, idx) => (
            <div
              key={idx}
              className="bg-neutral-50 rounded-2xl p-8 shadow-sm border border-neutral-100 hover:shadow-md transition"
            >
              <div className="h-12 w-12 rounded-2xl bg-orange-50 flex items-center justify-center text-primary-600 text-xl">
                {b.icon}
              </div>

              <h3 className="mt-6 text-lg font-bold text-neutral-900">{b.title}</h3>

              <p className="mt-4 text-sm leading-relaxed text-neutral-600">{b.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
