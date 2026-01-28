/* eslint-disable @typescript-eslint/no-explicit-any */
type LinkItem = { label: string; href: string };
import { BadgeCheck, Facebook, Instagram, Linkedin, Lock, Youtube } from 'lucide-react';
import logotipo from '../../assets/logotipo.png';

export default function Footer() {
  const products: LinkItem[] = [
    { label: 'Casas', href: '/produtos' },
    { label: 'Chalés', href: '/produtos' },
  ];

  const company: LinkItem[] = [
    { label: 'Sobre Nós', href: '/sobre' },
    { label: 'Como Funciona', href: '/sobre' },
    { label: 'Certificações', href: '#' },
    { label: 'Carreiras', href: '#' },
  ];

  const support: LinkItem[] = [
    { label: 'FAQ', href: '#' },
    { label: 'Contato', href: '/contato' },
    { label: 'Termos de Serviço', href: '/legal/termos' },
    { label: 'Política de Privacidade', href: '/legal/privacidade' },
  ];

  return (
    <footer className="bg-slate-950 text-slate-200">
      <div className="max-w-7xl mx-auto px-6 pt-14 pb-8">
        <div className="grid gap-12 lg:grid-cols-4">
          {/* Brand */}
          <div>
            <div className="flex items-center gap-3">
              <img
                src={logotipo}
                alt="PlantaHUB logo"
                className="h-10 w-10 rounded-lg object-contain"
              />
              <span className="font-semibold text-lg text-white">PlantaHUB</span>
            </div>

            <p className="mt-4 text-sm text-slate-400 leading-relaxed max-w-sm">
              Plantas arquitetônicas profissionais para projetos residenciais. Certificadas por
              CAU/CREA com documentação técnica completa.
            </p>

            {/* Social */}
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

          {/* Columns */}
          <FooterColumn title="Produtos" links={products} />
          <FooterColumn title="Empresa" links={company} />
          <FooterColumn title="Suporte" links={support} />
        </div>

        {/* Divider */}
        <div className="mt-12 border-t border-slate-800/70" />

        {/* Bottom bar */}
        <div className="mt-6 flex flex-col gap-4 md:flex-row md:items-center md:justify-between text-sm text-slate-400">
          <span>© {new Date().getFullYear()} PlantaHUB. Todos os direitos reservados.</span>

          <div className="flex items-center gap-6">
            <Badge icon={Lock} label="SSL Secured" />
            <Badge icon={BadgeCheck} label="CAU/CREA Certified" />
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
        {links.map(l => (
          <li key={l.label}>
            <a href={l.href} className="text-sm text-slate-400 hover:text-white transition">
              {l.label}
            </a>
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
      className="h-9 w-9 rounded-lg bg-slate-900 border border-slate-800
                 text-slate-300 hover:text-white hover:border-slate-700
                 transition flex items-center justify-center"
    >
      <Icon className="h-4 w-4" />
    </a>
  );
}

function Badge({ icon: Icon, label }: any) {
  return (
    <div className="inline-flex items-center gap-2 text-slate-400">
      <Icon className="h-4 w-4 text-primary-500" />
      <span>{label}</span>
    </div>
  );
}
