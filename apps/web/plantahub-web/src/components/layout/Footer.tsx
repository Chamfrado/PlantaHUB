import { BadgeCheck, Facebook, Instagram, Linkedin, Lock, Youtube } from 'lucide-react';
import { Link } from 'react-router-dom';
import logotipo from '../../assets/logotipo.png';

type LinkItem = { label: string; href: string };

export default function Footer() {
  const products: LinkItem[] = [
    { label: 'Casas', href: '/produtos?category=casas' },
    { label: 'Chalés', href: '/produtos?category=chales' },
  ];

  const company: LinkItem[] = [
    { label: 'Sobre Nós', href: '/sobre' },
    { label: 'Como Funciona', href: '/sobre' },
    { label: 'Trabalhe Conosco', href: '/trabalhe-conosco' },
  ];

  const support: LinkItem[] = [
    { label: 'FAQ', href: '/faq' },
    { label: 'Contato', href: '/contato' },
    { label: 'Termos de Serviço', href: '/legal/termos' },
    { label: 'Política de Privacidade', href: '/legal/privacidade' },
  ];

  return (
    <footer className="bg-slate-950 text-slate-200">
      <div className="mx-auto max-w-7xl px-6 pb-8 pt-14">
        <div className="grid gap-12 lg:grid-cols-4">
          <div>
            <div className="flex items-center gap-3">
              <img
                src={logotipo}
                alt="PlantaHUB logo"
                className="h-10 w-10 rounded-lg object-contain"
              />
              <span className="text-lg font-semibold text-white">PlantaHUB</span>
            </div>

            <p className="mt-4 max-w-sm text-sm leading-relaxed text-slate-400">
              Plantas arquitetônicas profissionais para projetos residenciais. Documentação técnica
              organizada, experiência digital e acesso simplificado à sua biblioteca.
            </p>

            <div className="mt-6 flex items-center gap-3">
              <SocialLink
                href="https://www.facebook.com/profile.php?id=61585474105574"
                label="Facebook"
                icon={Facebook}
              />

              <SocialLink
                href="https://www.instagram.com/planta_hub/"
                label="Instagram"
                icon={Instagram}
              />

              <SocialLink
                href="https://www.linkedin.com/company/plantahub/?viewAsMember=true"
                label="LinkedIn"
                icon={Linkedin}
              />

              <SocialLink
                href="https://www.youtube.com/@PlantaHub"
                label="YouTube"
                icon={Youtube}
              />
            </div>
          </div>

          <FooterColumn title="Produtos" links={products} />
          <FooterColumn title="Empresa" links={company} />
          <FooterColumn title="Suporte" links={support} />
        </div>

        <div className="mt-12 border-t border-slate-800/70" />

        <div className="mt-6 flex flex-col gap-4 text-sm text-slate-400 md:flex-row md:items-center md:justify-between">
          <span>© {new Date().getFullYear()} PlantaHUB. Todos os direitos reservados.</span>

          <div className="flex items-center gap-6">
            <Badge icon={Lock} label="SSL Secured" />
            <Badge icon={BadgeCheck} label="Trusted Platform" />
          </div>
        </div>
      </div>
    </footer>
  );
}

function FooterColumn({ title, links }: { title: string; links: LinkItem[] }) {
  return (
    <div>
      <div className="font-semibold text-slate-200">{title}</div>
      <ul className="mt-4 space-y-3">
        {links.map(link => (
          <li key={link.label}>
            <Link to={link.href} className="text-sm text-slate-400 transition hover:text-white">
              {link.label}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}

function SocialLink({
  href,
  label,
  icon: Icon,
}: {
  href: string;
  label: string;
  icon: React.ElementType;
}) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={label}
      className="flex h-9 w-9 items-center justify-center rounded-lg border border-slate-800 bg-slate-900 text-slate-300 transition hover:border-slate-700 hover:text-white"
    >
      <Icon className="h-4 w-4" />
    </a>
  );
}

function Badge({ icon: Icon, label }: { icon: React.ElementType; label: string }) {
  return (
    <div className="inline-flex items-center gap-2 text-slate-400">
      <Icon className="h-4 w-4 text-primary-500" />
      <span>{label}</span>
    </div>
  );
}
