import React, { useState, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import client from '../api/client';
import { paymentsApi } from '../api/payments';
import { subscriptionApi } from '../api/subscription';
import { User as UserIcon, Upload, CheckCircle2, AlertCircle, CreditCard, Bell, Loader2 } from 'lucide-react';
import type { User } from '../types';

const profileSchema = z.object({
  fullName: z.string().min(2, 'Name must be at least 2 characters'),
  businessName: z.string().min(1, 'Business name is required'),
  businessAddress: z.string().min(1, 'Business address is required'),
  taxId: z.string().optional(),
  defaultCurrency: z.string().min(3, 'Select a currency'),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

type Tab = 'profile' | 'billing';

const PLANS = [
  {
    id: 'FREE' as const,
    name: 'Free',
    price: '$0',
    features: ['5 invoices per month', '3 clients', 'Basic PDF export', 'Email support'],
  },
  {
    id: 'STARTER' as const,
    name: 'Starter',
    price: '$12/mo',
    features: ['Unlimited invoices', 'Unlimited clients', 'Proposal templates', 'Payment reminders'],
  },
  {
    id: 'PRO' as const,
    name: 'Pro',
    price: '$29/mo',
    features: ['Everything in Starter', 'Priority support', 'Advanced reporting', 'Custom branding'],
  },
];

const SettingsPage: React.FC = () => {
  const { user, updateUser } = useAuth();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<Tab>('profile');

  const [success, setSuccess] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState<boolean>(false);
  const [logoUploading, setLogoUploading] = useState<boolean>(false);
  const [logoError, setLogoError] = useState(false);

  React.useEffect(() => {
    setLogoError(false);
  }, [user?.businessLogoUrl]);
  const [connectingChapa, setConnectingChapa] = useState(false);
  const [chapaSecretKey, setChapaSecretKey] = useState('');
  const [remindersEnabled, setRemindersEnabled] = useState(user?.remindersEnabled ?? true);
  const [reminderDaysBeforeDue, setReminderDaysBeforeDue] = useState(user?.reminderDaysBeforeDue ?? 3);
  const [reminderDaysAfterDue, setReminderDaysAfterDue] = useState(user?.reminderDaysAfterDue ?? 7);
  const [savingNotifications, setSavingNotifications] = useState(false);
  const [billingAction, setBillingAction] = useState<string | null>(null);

  const subscriptionQuery = useQuery({
    queryKey: ['subscription'],
    queryFn: subscriptionApi.getSubscriptionStatus,
    enabled: activeTab === 'billing',
  });

  useEffect(() => {
    if (!user) return;
    setRemindersEnabled(user.remindersEnabled);
    setReminderDaysBeforeDue(user.reminderDaysBeforeDue);
    setReminderDaysAfterDue(user.reminderDaysAfterDue);
  }, [user]);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      fullName: user?.fullName || '',
      businessName: user?.businessName || '',
      businessAddress: user?.businessAddress || '',
      taxId: user?.taxId || '',
      defaultCurrency: user?.defaultCurrency || 'USD',
    },
  });

  const onSubmit = async (data: ProfileFormValues) => {
    setSuccess(null);
    setError(null);
    setSaving(true);
    try {
      const response = await client.put<User>('/api/v1/users/me', data);
      updateUser(response.data);
      setSuccess('Business profile updated successfully!');
    } catch (err: any) {
      console.error(err);
      setError('Failed to update business profile. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleLogoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0) return;
    const file = e.target.files[0];

    // Check client-side validation
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      setError('Only JPEG and PNG formats are supported.');
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setError('File size must be less than 2MB.');
      return;
    }

    setSuccess(null);
    setError(null);
    setLogoUploading(true);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await client.post<User>('/api/v1/users/me/logo', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      updateUser(response.data);
      setSuccess('Logo uploaded successfully!');
    } catch (err: any) {
      console.error(err);
      setError('Failed to upload business logo. Please check file format.');
    } finally {
      setLogoUploading(false);
    }
  };

  const handleChapaConnect = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!chapaSecretKey.trim()) return;
    setConnectingChapa(true);
    setError(null);
    setSuccess(null);
    try {
      await paymentsApi.connectChapa(chapaSecretKey);
      setSuccess('Chapa Secret Key saved successfully!');
      setChapaSecretKey('');
      const me = await client.get<User>('/api/v1/users/me');
      updateUser(me.data);
    } catch {
      setError('Failed to save Chapa Secret Key. Please try again.');
    } finally {
      setConnectingChapa(false);
    }
  };

  const handleSaveNotifications = async () => {
    setSavingNotifications(true);
    setError(null);
    setSuccess(null);
    try {
      await paymentsApi.updateNotificationSettings({
        remindersEnabled,
        reminderDaysBeforeDue,
        reminderDaysAfterDue,
      });
      const me = await client.get<User>('/api/v1/users/me');
      updateUser(me.data);
      setSuccess('Notification settings saved.');
    } catch {
      setError('Failed to save notification settings.');
    } finally {
      setSavingNotifications(false);
    }
  };

  const handleUpgrade = async (plan: 'STARTER' | 'PRO') => {
    setBillingAction(plan);
    setError(null);
    setSuccess(null);
    try {
      const status = await subscriptionApi.upgradePlan(plan);
      queryClient.invalidateQueries({ queryKey: ['subscription'] });
      updateUser({ ...user!, plan: status.plan });
      setSuccess(`Upgraded to ${plan} plan successfully!`);
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to upgrade plan.');
    } finally {
      setBillingAction(null);
    }
  };

  const handleCancelSubscription = async () => {
    if (!confirm('Cancel your subscription? You will be downgraded to the Free plan.')) return;
    setBillingAction('cancel');
    setError(null);
    setSuccess(null);
    try {
      const status = await subscriptionApi.cancelSubscription();
      queryClient.invalidateQueries({ queryKey: ['subscription'] });
      updateUser({ ...user!, plan: status.plan });
      setSuccess('Subscription cancelled. You are now on the Free plan.');
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to cancel subscription.');
    } finally {
      setBillingAction(null);
    }
  };

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-6">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Account & Billing Settings</h1>
          <p className="text-xs sm:text-sm text-zinc-400 mt-0.5">Configure your company identity, payment gateways, and subscription tiers.</p>
        </div>

        <div className="flex gap-2 border-b border-white/[0.08] pb-0">
          {(['profile', 'billing'] as Tab[]).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-2.5 text-xs sm:text-sm font-semibold capitalize transition-all border-b-2 -mb-px ${
                activeTab === tab
                  ? 'text-indigo-400 border-indigo-500'
                  : 'text-zinc-400 border-transparent hover:text-zinc-200'
              }`}
            >
              {tab === 'profile' ? 'Business Profile' : 'Plans & Subscriptions'}
            </button>
          ))}
        </div>

        {success && (
          <div className="p-4 bg-emerald-500/10 border border-emerald-500/25 rounded-2xl flex items-center gap-3 text-emerald-400 text-xs sm:text-sm font-medium">
            <CheckCircle2 className="w-5 h-5 shrink-0" />
            <span>{success}</span>
          </div>
        )}

        {error && (
          <div className="p-4 bg-rose-500/10 border border-rose-500/25 rounded-2xl flex items-center gap-3 text-rose-400 text-xs sm:text-sm font-medium">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {activeTab === 'profile' && (
        <>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Logo Upload Card */}
          <div className="bg-[#0a0e1c] border border-white/[0.08] p-6 rounded-2xl flex flex-col items-center justify-center text-center shadow-sm">
            <h3 className="text-[11px] font-bold text-zinc-400 uppercase tracking-wider mb-6">Business Logo</h3>
            <div className="relative group w-32 h-32 rounded-2xl bg-white/[0.02] border border-white/[0.08] flex items-center justify-center text-zinc-500 overflow-hidden mb-4">
              {user?.businessLogoUrl && !logoError ? (
                <img
                  src={user.businessLogoUrl}
                  alt="Logo"
                  onError={() => setLogoError(true)}
                  className="w-full h-full object-cover"
                />
              ) : (
                <UserIcon className="w-12 h-12 text-zinc-600" />
              )}
              {logoUploading && (
                <div className="absolute inset-0 bg-[#070913]/80 flex items-center justify-center">
                  <span className="w-6 h-6 border-2 border-indigo-500/20 border-t-indigo-500 rounded-full animate-spin"></span>
                </div>
              )}
            </div>
            <label className="cursor-pointer px-4 py-2.5 bg-white/[0.03] hover:bg-white/[0.06] border border-white/[0.08] text-zinc-200 text-xs font-semibold rounded-xl flex items-center gap-2 transition-all">
              <Upload className="w-4 h-4" />
              Upload Logo
              <input type="file" onChange={handleLogoUpload} className="hidden" accept="image/png, image/jpeg" />
            </label>
            <p className="text-[10px] text-zinc-500 mt-3 font-mono-num">PNG or JPEG format up to 2MB</p>
          </div>

          {/* Profile Details Form */}
          <div className="lg:col-span-2 bg-[#0a0e1c] border border-white/[0.08] p-7 rounded-2xl shadow-sm">
            <h3 className="text-[11px] font-bold text-zinc-400 uppercase tracking-wider mb-6">Company Credentials</h3>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-zinc-300 block">Owner Full Name</label>
                  <input
                    type="text"
                    {...register('fullName')}
                    className={`w-full px-4 py-2.5 bg-white/[0.02] hover:bg-white/[0.03] border ${
                      errors.fullName ? 'border-rose-500/60' : 'border-white/[0.08] focus:border-indigo-500/70'
                    } rounded-xl text-white text-xs sm:text-sm placeholder-zinc-500 focus:outline-none transition-all`}
                  />
                  {errors.fullName && <p className="text-rose-400 text-xs mt-1">{errors.fullName.message}</p>}
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-zinc-300 block">Business Name</label>
                  <input
                    type="text"
                    {...register('businessName')}
                    className={`w-full px-4 py-2.5 bg-white/[0.02] hover:bg-white/[0.03] border ${
                      errors.businessName ? 'border-rose-500/60' : 'border-white/[0.08] focus:border-indigo-500/70'
                    } rounded-xl text-white text-xs sm:text-sm placeholder-zinc-500 focus:outline-none transition-all`}
                    placeholder="Doe Consulting LLC"
                  />
                  {errors.businessName && <p className="text-rose-400 text-xs mt-1">{errors.businessName.message}</p>}
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-zinc-300 block">Business Address</label>
                <textarea
                  {...register('businessAddress')}
                  rows={3}
                  className={`w-full px-4 py-2.5 bg-white/[0.02] hover:bg-white/[0.03] border ${
                    errors.businessAddress ? 'border-rose-500/60' : 'border-white/[0.08] focus:border-indigo-500/70'
                  } rounded-xl text-white text-xs sm:text-sm placeholder-zinc-500 focus:outline-none transition-all resize-none`}
                  placeholder="123 Financial Way, Suite 400&#10;San Francisco, CA 94111"
                />
                {errors.businessAddress && <p className="text-rose-400 text-xs mt-1">{errors.businessAddress.message}</p>}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-zinc-300 block">Tax ID / EIN</label>
                  <input
                    type="text"
                    {...register('taxId')}
                    className="w-full px-4 py-2.5 bg-white/[0.02] hover:bg-white/[0.03] border border-white/[0.08] focus:border-indigo-500/70 rounded-xl text-white text-xs sm:text-sm placeholder-zinc-500 focus:outline-none transition-all font-mono-num"
                    placeholder="XX-XXXXXXX"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-zinc-300 block">Default Currency</label>
                  <select
                    {...register('defaultCurrency')}
                    className="w-full px-4 py-2.5 bg-[#0a0e1c] border border-white/[0.08] focus:border-indigo-500/70 rounded-xl text-white text-xs sm:text-sm focus:outline-none transition-all font-mono-num"
                  >
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (€)</option>
                    <option value="GBP">GBP (£)</option>
                    <option value="CAD">CAD ($)</option>
                    <option value="AUD">AUD ($)</option>
                  </select>
                </div>
              </div>

              <div className="flex justify-end pt-3">
                <button
                  type="submit"
                  disabled={saving}
                  className="px-6 py-2.5 bg-gradient-to-r from-indigo-500 via-indigo-600 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-semibold text-xs sm:text-sm rounded-xl shadow-lg shadow-indigo-500/20 disabled:opacity-50 active:scale-[0.98] transition-all"
                >
                  {saving ? 'Saving changes...' : 'Save Settings'}
                </button>
              </div>
            </form>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-[#0a0e1c] border border-white/[0.08] p-6 rounded-2xl shadow-sm">
            <div className="flex items-center gap-2 mb-3">
              <CreditCard className="w-5 h-5 text-indigo-400" />
              <h3 className="text-xs font-bold text-zinc-200 uppercase tracking-wider">Chapa Payments</h3>
            </div>
            <p className="text-xs text-zinc-400 mb-4 leading-relaxed">
              Enter your Chapa Secret Key to enable automated online credit card / mobile payments on public invoice links.
            </p>
            <form onSubmit={handleChapaConnect} className="space-y-3">
              <div>
                <input
                  type="password"
                  value={chapaSecretKey}
                  onChange={e => setChapaSecretKey(e.target.value)}
                  placeholder="CHASECK_TEST-..."
                  className="w-full px-3.5 py-2.5 bg-white/[0.02] border border-white/[0.08] rounded-xl text-xs text-zinc-200 focus:outline-none focus:border-indigo-500 font-mono-num"
                />
              </div>
              <button
                type="submit"
                disabled={connectingChapa || !chapaSecretKey.trim()}
                className="px-4 py-2 bg-indigo-500/20 border border-indigo-500/30 text-indigo-300 text-xs font-semibold rounded-xl hover:bg-indigo-500/30 transition-all disabled:opacity-50"
              >
                {connectingChapa ? 'Saving…' : 'Save Chapa Secret Key'}
              </button>
            </form>
            {user?.chapaConnected && (
              <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold mt-4">
                <CheckCircle2 className="w-4 h-4" /> Chapa gateway connected
              </div>
            )}
          </div>

          <div className="bg-[#0a0e1c] border border-white/[0.08] p-6 rounded-2xl shadow-sm">
            <div className="flex items-center gap-2 mb-4">
              <Bell className="w-5 h-5 text-indigo-400" />
              <h3 className="text-sm font-semibold text-zinc-200 uppercase tracking-wider">Reminder Schedule</h3>
            </div>
            <div className="space-y-4">
              <label className="flex items-center gap-2 text-sm text-zinc-300">
                <input type="checkbox" checked={remindersEnabled}
                  onChange={e => setRemindersEnabled(e.target.checked)}
                  className="rounded border-white/20" />
                Enable automatic invoice reminders
              </label>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs text-zinc-500 block mb-1">Days before due date</label>
                  <input type="number" min={0} value={reminderDaysBeforeDue}
                    onChange={e => setReminderDaysBeforeDue(parseInt(e.target.value) || 0)}
                    className="w-full px-3 py-2 bg-white/[0.02] border border-white/[0.08] rounded-lg text-sm text-zinc-200" />
                </div>
                <div>
                  <label className="text-xs text-zinc-500 block mb-1">Days after due date</label>
                  <input type="number" min={0} value={reminderDaysAfterDue}
                    onChange={e => setReminderDaysAfterDue(parseInt(e.target.value) || 0)}
                    className="w-full px-3 py-2 bg-white/[0.02] border border-white/[0.08] rounded-lg text-sm text-zinc-200" />
                </div>
              </div>
              <button onClick={handleSaveNotifications} disabled={savingNotifications}
                className="px-4 py-2 bg-white/[0.05] border border-white/[0.08] text-zinc-200 text-sm font-medium rounded-xl hover:bg-white/[0.08] disabled:opacity-50">
                {savingNotifications ? 'Saving…' : 'Save Reminder Settings'}
              </button>
            </div>
          </div>
        </div>
        </>
        )}

        {activeTab === 'billing' && (
          <div className="space-y-6">
            {subscriptionQuery.isLoading ? (
              <div className="flex justify-center py-12">
                <Loader2 className="w-8 h-8 text-indigo-400 animate-spin" />
              </div>
            ) : subscriptionQuery.data && (
              <div className="bg-[#0a0e1c] border border-white/[0.08] p-6 rounded-2xl shadow-sm">
                <h3 className="text-[11px] font-bold text-zinc-400 uppercase tracking-wider mb-4">Current Usage Tier</h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                  <div className="p-4 bg-white/[0.02] rounded-xl border border-white/[0.06]">
                    <p className="text-zinc-400">Invoices generated this month</p>
                    <p className="text-xl font-bold font-mono-num text-white mt-1">
                      {subscriptionQuery.data.plan === 'FREE'
                        ? `${subscriptionQuery.data.invoicesThisMonth} / 5`
                        : `${subscriptionQuery.data.invoicesThisMonth} (Unlimited)`}
                    </p>
                  </div>
                  <div className="p-4 bg-white/[0.02] rounded-xl border border-white/[0.06]">
                    <p className="text-zinc-400">Managed Clients</p>
                    <p className="text-xl font-bold font-mono-num text-white mt-1">
                      {subscriptionQuery.data.plan === 'FREE'
                        ? `${subscriptionQuery.data.totalClients} / 3`
                        : `${subscriptionQuery.data.totalClients} (Unlimited)`}
                    </p>
                  </div>
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {PLANS.map(plan => {
                const isActive = (subscriptionQuery.data?.plan ?? user?.plan) === plan.id;
                return (
                  <div
                    key={plan.id}
                    className={`bg-[#0a0e1c] border p-6 rounded-2xl flex flex-col justify-between transition-all duration-200 ${
                      isActive
                        ? 'border-indigo-500/50 bg-gradient-to-b from-indigo-500/[0.08] to-[#0a0e1c] shadow-lg shadow-indigo-500/10 ring-1 ring-indigo-500/20'
                        : 'border-white/[0.08] hover:border-white/[0.14]'
                    }`}
                  >
                    <div>
                      <div className="flex items-center justify-between">
                        <h3 className="text-base font-bold text-white">{plan.name}</h3>
                        {isActive && (
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-500/20 border border-indigo-500/30 text-indigo-300 uppercase tracking-wider">
                            Active
                          </span>
                        )}
                      </div>
                      <p className="text-2xl font-extrabold text-white mt-2 font-mono-num">{plan.price}</p>
                      <ul className="mt-5 space-y-2.5">
                        {plan.features.map(f => (
                          <li key={f} className="text-xs text-zinc-300 flex items-start gap-2">
                            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                            <span>{f}</span>
                          </li>
                        ))}
                      </ul>
                    </div>

                    {!isActive && plan.id !== 'FREE' && (
                      <button
                        onClick={() => handleUpgrade(plan.id)}
                        disabled={!!billingAction}
                        className="mt-6 w-full px-4 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white text-xs font-semibold rounded-xl disabled:opacity-50 flex items-center justify-center gap-2 shadow-md shadow-indigo-500/20 active:scale-[0.98] transition-all"
                      >
                        {billingAction === plan.id ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                        Upgrade to {plan.name}
                      </button>
                    )}
                  </div>
                );
              })}
            </div>

            {(subscriptionQuery.data?.plan === 'STARTER' || subscriptionQuery.data?.plan === 'PRO') && (
              <div className="flex justify-end pt-2">
                <button
                  onClick={handleCancelSubscription}
                  disabled={!!billingAction}
                  className="px-4 py-2 bg-rose-500/10 border border-rose-500/25 text-rose-300 text-xs font-semibold rounded-xl hover:bg-rose-500/20 disabled:opacity-50 flex items-center gap-2 transition-all"
                >
                  {billingAction === 'cancel' ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                  Downgrade to Free Tier
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default SettingsPage;
