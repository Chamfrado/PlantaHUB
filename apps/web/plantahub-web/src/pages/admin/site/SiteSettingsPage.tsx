import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  AdminPageHeader,
  DataError,
  FormField,
  PrimaryButton,
  TextInput,
} from '../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../lib/api-error';
import {
  getAdminSiteSettings,
  updateSiteSettings,
} from '../../../services/admin/admin.service';
import type { SiteSettings } from '../../../types/api/site';

const EMPTY: SiteSettings = {
  email: null,
  partnershipsEmail: null,
  phone: null,
  whatsapp: null,
  address: null,
  businessHours: null,
  instagramUrl: null,
  facebookUrl: null,
  linkedinUrl: null,
  youtubeUrl: null,
};

/**
 * Contatos e redes sociais.
 *
 * <p>Existe por causa de um defeito concreto: estes dados estavam escritos em três
 * componentes e divergiam entre si — o rodapé apontava para um Instagram e a página de
 * Contato para outro. Com uma fonte só, corrigir passa a ser uma edição, e a divergência
 * deixa de estar disponível.
 *
 * <p>Campo vazio não aparece no site. É deliberado: melhor um contato a menos do que um
 * telefone de exemplo, que foi exatamente o que ficou no ar.
 */
export default function SiteSettingsPage() {
  const settings = useQuery({
    queryKey: ['admin', 'site', 'settings'],
    queryFn: getAdminSiteSettings,
  });

  if (settings.isLoading) {
    return <div className="h-96 animate-pulse rounded-2xl bg-white" />;
  }

  if (settings.isError) {
    return (
      <DataError
        message={getApiErrorMessage(settings.error, 'Não foi possível carregar as configurações.')}
        onRetry={() => void settings.refetch()}
      />
    );
  }

  // O formulário nasce com os dados já em mãos, em vez de copiá-los num efeito depois.
  // Além de ser o que a regra do lint pede, evita que uma revalidação em segundo plano
  // apague o que a pessoa estiver digitando.
  return <SettingsForm initial={settings.data ?? EMPTY} />;
}

function SettingsForm({ initial }: { initial: SiteSettings }) {
  const queryClient = useQueryClient();

  const [form, setForm] = useState<SiteSettings>(initial);
  const [saved, setSaved] = useState(false);

  const save = useMutation({
    mutationFn: () => updateSiteSettings(form),
    onSuccess: () => {
      setSaved(true);
      window.setTimeout(() => setSaved(false), 3000);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'site', 'settings'] });
    },
  });

  function field(key: keyof SiteSettings) {
    return {
      value: form[key] ?? '',
      onChange: (event: React.ChangeEvent<HTMLInputElement>) =>
        setForm(previous => ({ ...previous, [key]: event.target.value || null })),
    };
  }

  return (
    <div className="max-w-3xl space-y-6">
      <AdminPageHeader
        title="Contato e redes"
        description="Usado no rodapé de todas as páginas e na tela de Contato."
        actions={
          <PrimaryButton onClick={() => save.mutate()} disabled={save.isPending}>
            {save.isPending ? 'Salvando…' : 'Salvar'}
          </PrimaryButton>
        }
      />

      {save.error ? (
        <DataError message={getApiErrorMessage(save.error, 'Não foi possível salvar.')} />
      ) : null}

      {saved ? (
        <p className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-800">
          Salvo. O rodapé e a página de Contato já usam estes dados.
        </p>
      ) : null}

      <section className="space-y-4 rounded-2xl border border-neutral-200 bg-white p-6">
        <h2 className="text-base font-extrabold text-neutral-900">Contato</h2>

        <FormField label="E-mail" htmlFor="site-email">
          <TextInput id="site-email" type="email" {...field('email')} />
        </FormField>

        <FormField
          label="E-mail de parcerias"
          htmlFor="site-partnerships"
          hint="Usado na página Trabalhe Conosco."
        >
          <TextInput id="site-partnerships" type="email" {...field('partnershipsEmail')} />
        </FormField>

        <FormField label="Telefone" htmlFor="site-phone">
          <TextInput id="site-phone" placeholder="(11) 98888-7777" {...field('phone')} />
        </FormField>

        <FormField
          label="WhatsApp"
          htmlFor="site-whatsapp"
          hint="Só números, com país e DDD: 5511988887777. O link é montado a partir daqui."
        >
          <TextInput id="site-whatsapp" placeholder="5511988887777" {...field('whatsapp')} />
        </FormField>

        <FormField label="Localização" htmlFor="site-address">
          <TextInput id="site-address" {...field('address')} />
        </FormField>

        <FormField label="Horário de atendimento" htmlFor="site-hours">
          <TextInput id="site-hours" placeholder="09:00 — 18:00" {...field('businessHours')} />
        </FormField>
      </section>

      <section className="space-y-4 rounded-2xl border border-neutral-200 bg-white p-6">
        <h2 className="text-base font-extrabold text-neutral-900">Redes sociais</h2>
        <p className="text-xs text-neutral-500">
          Endereço completo, com https. Rede sem endereço não aparece no site.
        </p>

        <FormField label="Instagram" htmlFor="site-instagram">
          <TextInput id="site-instagram" {...field('instagramUrl')} />
        </FormField>

        <FormField label="Facebook" htmlFor="site-facebook">
          <TextInput id="site-facebook" {...field('facebookUrl')} />
        </FormField>

        <FormField label="LinkedIn" htmlFor="site-linkedin">
          <TextInput id="site-linkedin" {...field('linkedinUrl')} />
        </FormField>

        <FormField label="YouTube" htmlFor="site-youtube">
          <TextInput id="site-youtube" {...field('youtubeUrl')} />
        </FormField>
      </section>
    </div>
  );
}
