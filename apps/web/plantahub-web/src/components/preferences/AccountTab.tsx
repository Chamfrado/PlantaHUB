import {
  AlertTriangle,
  Eye,
  EyeOff,
  Pencil,
  Save,
  Shield,
  Trash2,
  UserCircle,
  X,
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { deleteMyProfile, getMyProfile, updateMyProfile } from '../../services/profile.service';

const DELETE_CONFIRM_TEXT = 'eu quero excluir minha conta';

export default function AccountTab() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);

  const [cpfLocked, setCpfLocked] = useState(false);

  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [showSensitiveData, setShowSensitiveData] = useState(false);
  const [isEditingSensitive, setIsEditingSensitive] = useState(false);

  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [deleteConfirmation, setDeleteConfirmation] = useState('');

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    cpf: '',
    phoneNumber: '',
  });

  const [originalSensitive, setOriginalSensitive] = useState({
    cpf: '',
    phoneNumber: '',
  });

  useEffect(() => {
    async function loadProfile() {
      try {
        setLoading(true);
        setError(null);

        const profile = await getMyProfile();

        const nextForm = {
          fullName: profile.fullName ?? '',
          email: profile.email ?? '',
          cpf: profile.cpf ?? '',
          phoneNumber: profile.phoneNumber ?? '',
        };

        setForm(nextForm);
        setOriginalSensitive({
          cpf: nextForm.cpf,
          phoneNumber: nextForm.phoneNumber,
        });
        setCpfLocked(profile.cpfLocked ?? !!nextForm.cpf);
      } catch (err) {
        console.error(err);
        setError('Não foi possível carregar os dados da conta.');
      } finally {
        setLoading(false);
      }
    }

    loadProfile();
  }, []);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    try {
      setSaving(true);
      setError(null);
      setSuccess(null);

      const updated = await updateMyProfile({
        fullName: form.fullName,
        cpf: cpfLocked ? undefined : form.cpf || undefined,
        phoneNumber: form.phoneNumber || undefined,
      });
      const nextForm = {
        fullName: updated.fullName ?? '',
        email: updated.email ?? '',
        cpf: updated.cpf ?? '',
        phoneNumber: updated.phoneNumber ?? '',
      };

      setForm(nextForm);
      setOriginalSensitive({
        cpf: nextForm.cpf,
        phoneNumber: nextForm.phoneNumber,
      });

      setIsEditingSensitive(false);
      setSuccess('Dados atualizados com sucesso.');
      setCpfLocked(updated.cpfLocked ?? !!updated.cpf);
    } catch (err) {
      console.error(err);
      setError('Não foi possível salvar as alterações.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteAccount() {
    try {
      setDeleteLoading(true);
      setError(null);

      await deleteMyProfile({
        confirmationText: deleteConfirmation.trim(),
      });

      logout();
      setDeleteModalOpen(false);
      setDeleteConfirmation('');
      navigate('/');
    } catch (err) {
      console.error(err);
      const message = err instanceof Error ? err.message : 'Não foi possível excluir a conta.';
      setError(message);
    } finally {
      setDeleteLoading(false);
    }
  }

  function handleStartSensitiveEdit() {
    setIsEditingSensitive(true);
    setShowSensitiveData(true);
    setSuccess(null);
    setError(null);
  }

  function handleCancelSensitiveEdit() {
    setForm(prev => ({
      ...prev,
      cpf: originalSensitive.cpf,
      phoneNumber: originalSensitive.phoneNumber,
    }));
    setIsEditingSensitive(false);
    setSuccess(null);
    setError(null);
  }

  const completionText = useMemo(() => {
    const fields = [form.fullName, form.email, form.cpf, form.phoneNumber].filter(Boolean).length;
    return fields === 4 ? 'Perfil completo' : 'Perfil incompleto';
  }, [form]);

  const canDeleteAccount = deleteConfirmation.trim().toLowerCase() === DELETE_CONFIRM_TEXT;

  if (loading) {
    return <CardSkeleton text="Carregando dados da conta..." />;
  }

  return (
    <>
      <div className="space-y-6">
        <SectionCard
          icon={<UserCircle className="h-5 w-5" />}
          title="Dados da conta"
          description="Atualize suas informações principais de cadastro."
          rightAction={
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => setShowSensitiveData(prev => !prev)}
                className="inline-flex items-center gap-2 rounded-xl border border-neutral-200 bg-white px-3 py-2 text-sm font-semibold text-neutral-700 hover:bg-neutral-50 transition"
              >
                {showSensitiveData ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                {showSensitiveData ? 'Ocultar dados' : 'Mostrar dados'}
              </button>

              {!isEditingSensitive ? (
                <button
                  type="button"
                  onClick={handleStartSensitiveEdit}
                  className="inline-flex items-center gap-2 rounded-xl border border-neutral-200 bg-white px-3 py-2 text-sm font-semibold text-neutral-700 hover:bg-neutral-50 transition"
                >
                  <Pencil className="h-4 w-4" />
                  {cpfLocked ? 'Editar telefone' : 'Editar dados sensíveis'}
                </button>
              ) : (
                <button
                  type="button"
                  onClick={handleCancelSensitiveEdit}
                  className="inline-flex items-center gap-2 rounded-xl border border-neutral-200 bg-white px-3 py-2 text-sm font-semibold text-neutral-700 hover:bg-neutral-50 transition"
                >
                  <X className="h-4 w-4" />
                  Cancelar edição
                </button>
              )}
            </div>
          }
        >
          <form onSubmit={handleSubmit} className="grid gap-5 md:grid-cols-2">
            <Field
              label="Nome completo"
              value={form.fullName}
              onChange={value => setForm(prev => ({ ...prev, fullName: value }))}
              placeholder="Seu nome completo"
            />

            <Field
              label="E-mail"
              value={form.email}
              onChange={() => {}}
              placeholder="seuemail@exemplo.com"
              type="email"
              disabled
              helperText="A alteração de e-mail ainda não está disponível por esta tela."
            />

            <SensitiveField
              label="CPF"
              realValue={form.cpf}
              displayValue={showSensitiveData ? form.cpf : maskCpf(form.cpf)}
              onChange={value => setForm(prev => ({ ...prev, cpf: value }))}
              placeholder="000.000.000-00"
              editable={!cpfLocked && isEditingSensitive}
              helperText={
                cpfLocked
                  ? 'Após preenchido, o CPF não pode mais ser alterado.'
                  : !isEditingSensitive
                    ? 'Campo protegido. Clique em “Editar” para preencher.'
                    : undefined
              }
            />

            <SensitiveField
              label="Telefone"
              realValue={form.phoneNumber}
              displayValue={showSensitiveData ? form.phoneNumber : maskPhone(form.phoneNumber)}
              onChange={value => setForm(prev => ({ ...prev, phoneNumber: value }))}
              placeholder="(00) 00000-0000"
              editable={isEditingSensitive}
              helperText={
                !isEditingSensitive
                  ? 'Campo protegido. Clique em “Editar” para alterar.'
                  : undefined
              }
            />

            <div className="md:col-span-2 flex flex-wrap items-center justify-between gap-3">
              <div className="text-sm font-medium text-neutral-500">{completionText}</div>

              <button
                type="submit"
                disabled={saving}
                className="inline-flex items-center gap-2 rounded-xl bg-primary-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-primary-600 disabled:opacity-70"
              >
                <Save className="h-4 w-4" />
                {saving ? 'Salvando...' : 'Salvar alterações'}
              </button>
            </div>
          </form>

          {success ? <p className="mt-4 text-sm font-medium text-green-600">{success}</p> : null}
          {error ? <p className="mt-4 text-sm font-medium text-red-600">{error}</p> : null}
        </SectionCard>

        <SectionCard
          icon={<Shield className="h-5 w-5" />}
          title="Segurança"
          description="Troca de senha e atualização de e-mail podem ser adicionadas como próxima etapa."
        >
          <div className="rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-600">
            No momento, sua API expõe atualização de perfil por <strong>/v1/me/profile</strong>, com
            suporte para nome completo, CPF e telefone. Para mudar senha e e-mail por esta tela, o
            ideal é criar endpoints dedicados depois.
          </div>
        </SectionCard>

        <DangerZoneCard onDeleteClick={() => setDeleteModalOpen(true)} />
      </div>

      <DeleteAccountModal
        open={deleteModalOpen}
        value={deleteConfirmation}
        loading={deleteLoading}
        canConfirm={canDeleteAccount}
        onChange={setDeleteConfirmation}
        onClose={() => {
          if (deleteLoading) return;
          setDeleteModalOpen(false);
          setDeleteConfirmation('');
        }}
        onConfirm={handleDeleteAccount}
      />
    </>
  );
}

function SensitiveField({
  label,
  realValue,
  displayValue,
  onChange,
  placeholder,
  editable,
  helperText,
}: {
  label: string;
  realValue: string;
  displayValue: string;
  onChange: (value: string) => void;
  placeholder?: string;
  editable: boolean;
  helperText?: string;
}) {
  return (
    <div>
      <label className="mb-2 block text-sm font-semibold text-neutral-800">{label}</label>

      <input
        type="text"
        value={editable ? realValue : displayValue}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        readOnly={!editable}
        className={[
          'w-full rounded-xl border px-4 py-3 text-sm outline-none transition placeholder:text-neutral-400',
          editable
            ? 'border-neutral-300 bg-white text-neutral-900 focus:border-primary-500'
            : 'border-neutral-200 bg-neutral-50 text-neutral-600 cursor-default',
        ].join(' ')}
      />

      {helperText ? <p className="mt-2 text-xs text-neutral-500">{helperText}</p> : null}
    </div>
  );
}

function DangerZoneCard({ onDeleteClick }: { onDeleteClick: () => void }) {
  return (
    <div className="rounded-2xl border border-red-200 bg-white p-6 md:p-8 shadow-sm">
      <div className="flex items-start gap-4">
        <div className="inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-red-50 text-red-600">
          <AlertTriangle className="h-5 w-5" />
        </div>

        <div className="flex-1">
          <h2 className="text-xl font-extrabold text-neutral-900">Zona de perigo</h2>
          <p className="mt-1 text-sm text-neutral-600">
            A exclusão da conta é permanente e não pode ser desfeita.
          </p>
        </div>
      </div>

      <div className="mt-8 rounded-2xl border border-red-200 bg-red-50/60 p-5">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h3 className="text-sm font-bold text-red-700">Excluir conta permanentemente</h3>
            <p className="mt-1 text-sm leading-relaxed text-red-700/90">
              Esta ação é irreversível. Seus acessos, histórico e dados associados à conta poderão
              ser perdidos de forma definitiva.
            </p>
          </div>

          <button
            type="button"
            onClick={onDeleteClick}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-red-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-red-700"
          >
            <Trash2 className="h-4 w-4" />
            Excluir conta
          </button>
        </div>
      </div>
    </div>
  );
}

function DeleteAccountModal({
  open,
  value,
  loading,
  canConfirm,
  onChange,
  onClose,
  onConfirm,
}: {
  open: boolean;
  value: string;
  loading: boolean;
  canConfirm: boolean;
  onChange: (value: string) => void;
  onClose: () => void;
  onConfirm: () => void;
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/45 px-4">
      <div className="w-full max-w-lg rounded-3xl border border-neutral-200 bg-white p-6 shadow-2xl">
        <div className="flex items-start gap-4">
          <div className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-red-50 text-red-600">
            <AlertTriangle className="h-6 w-6" />
          </div>

          <div>
            <h3 className="text-xl font-extrabold text-neutral-900">Confirmar exclusão da conta</h3>
            <p className="mt-2 text-sm leading-relaxed text-neutral-600">
              Esta ação é <strong>irreversível</strong>. Para confirmar, digite exatamente:
            </p>
            <p className="mt-2 rounded-xl bg-neutral-100 px-3 py-2 text-sm font-bold text-neutral-900">
              eu quero excluir minha conta
            </p>
          </div>
        </div>

        <div className="mt-6">
          <label className="mb-2 block text-sm font-semibold text-neutral-800">
            Frase de confirmação
          </label>
          <input
            value={value}
            onChange={e => onChange(e.target.value)}
            placeholder="Digite a frase de confirmação"
            className="w-full rounded-xl border border-neutral-300 bg-white px-4 py-3 text-sm text-neutral-900 outline-none transition placeholder:text-neutral-400 focus:border-red-500"
          />
        </div>

        <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          Ao continuar, sua conta poderá ser excluída de forma definitiva. Você perderá acesso aos
          dados vinculados a ela.
        </div>

        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="rounded-xl border border-neutral-300 bg-white px-5 py-3 text-sm font-semibold text-neutral-700 hover:bg-neutral-50 transition disabled:opacity-70"
          >
            Cancelar
          </button>

          <button
            type="button"
            onClick={onConfirm}
            disabled={!canConfirm || loading}
            className="rounded-xl bg-red-600 px-5 py-3 text-sm font-semibold text-white hover:bg-red-700 transition disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? 'Excluindo...' : 'Excluir conta permanentemente'}
          </button>
        </div>
      </div>
    </div>
  );
}

function SectionCard({
  icon,
  title,
  description,
  rightAction,
  children,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  rightAction?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-6 md:p-8 shadow-sm">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="flex items-start gap-4">
          <div className="inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-orange-50 text-primary-600">
            {icon}
          </div>

          <div>
            <h2 className="text-xl font-extrabold text-neutral-900">{title}</h2>
            <p className="mt-1 text-sm text-neutral-600">{description}</p>
          </div>
        </div>

        {rightAction ? <div>{rightAction}</div> : null}
      </div>

      <div className="mt-8">{children}</div>
    </div>
  );
}

function Field({
  label,
  value,
  onChange,
  placeholder,
  type = 'text',
  disabled = false,
  helperText,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  type?: string;
  disabled?: boolean;
  helperText?: string;
}) {
  return (
    <div>
      <label className="mb-2 block text-sm font-semibold text-neutral-800">{label}</label>

      <input
        type={type}
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        className="w-full rounded-xl border border-neutral-300 bg-white px-4 py-3 text-sm text-neutral-900 outline-none transition placeholder:text-neutral-400 focus:border-primary-500 disabled:bg-neutral-100 disabled:text-neutral-500"
      />

      {helperText ? <p className="mt-2 text-xs text-neutral-500">{helperText}</p> : null}
    </div>
  );
}

function maskCpf(value: string) {
  if (!value) return '';
  const digits = value.replace(/\D/g, '');
  if (digits.length < 4) return '***.***.***-**';
  return `***.***.***-${digits.slice(-2)}`;
}

function maskPhone(value: string) {
  if (!value) return '';
  const digits = value.replace(/\D/g, '');
  if (digits.length < 4) return '(**) *****-****';
  return `(**) *****-${digits.slice(-4)}`;
}

function CardSkeleton({ text }: { text: string }) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm">
      <p className="text-sm text-neutral-500">{text}</p>
    </div>
  );
}
