import type { SiteSection } from '../../types/api/site';
import { anchorOf } from './anchor';

/** Índice lateral: um link por seção com título. */
export function SectionIndex({ sections }: { sections: SiteSection[] }) {
  return (
    <nav className="mt-4 space-y-2 text-sm">
      {sections
        .filter(section => section.title)
        .map(section => (
          <a
            key={section.title}
            href={`#${anchorOf(section.title as string)}`}
            className="block rounded-xl px-3 py-2 text-neutral-700 transition hover:bg-neutral-50 hover:text-neutral-900"
          >
            {section.title}
          </a>
        ))}
    </nav>
  );
}

/**
 * As seções de uma página de prosa: Termos e Privacidade.
 *
 * O corpo e a lista são campos separados porque a página precisa saber o que é parágrafo e
 * o que é marcador para renderizar cada um do seu jeito. Juntar tudo num campo só obrigaria
 * a interpretar marcação no meio do texto.
 */
export default function SiteSections({ sections }: { sections: SiteSection[] }) {
  return (
    <div className="space-y-10">
      {sections.map((section, index) => (
        <section
          key={section.title ?? index}
          id={section.title ? anchorOf(section.title) : undefined}
          className="scroll-mt-24"
        >
          {section.title ? (
            <h2 className="text-xl font-extrabold text-neutral-900">{section.title}</h2>
          ) : null}

          <div className="mt-3 leading-relaxed text-neutral-700">
            {section.body ? <p>{section.body}</p> : null}

            {section.items.length > 0 ? (
              <ul className="mt-3 list-disc space-y-1 pl-5">
                {section.items.map((item, itemIndex) => (
                  <li key={item.title ?? item.text ?? itemIndex}>
                    {item.title ? <strong>{item.title}. </strong> : null}
                    {item.text}
                  </li>
                ))}
              </ul>
            ) : null}
          </div>

          <div className="mt-8 border-b border-neutral-200" />
        </section>
      ))}
    </div>
  );
}
