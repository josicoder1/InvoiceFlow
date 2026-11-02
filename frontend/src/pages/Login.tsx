import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import client from '../api/client';
import type { TokenResponse } from '../types';
import { KeyRound, Mail, AlertCircle, Eye, EyeOff } from 'lucide-react';

const loginSchema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [showPassword, setShowPassword] = useState<boolean>(false);

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  });

  const fillDemo = (email: string) => {
    setValue('email', email, { shouldValidate: true });
    setValue('password', 'password123', { shouldValidate: true });
  };

  const onSubmit = async (data: LoginFormValues) => {
    setError(null);
    setLoading(true);
    try {
      const response = await client.post<TokenResponse>('/api/v1/auth/login', {
        email: data.email,
        password: data.password,
      });
      await login(response.data.accessToken);
      navigate('/dashboard');
    } catch (err: any) {
      console.error(err);
      if (err.response?.data?.detail) {
        setError(err.response.data.detail);
      } else {
        setError('Invalid email or password. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#070913] flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Decorative Glows */}
      <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] rounded-full bg-indigo-600/[0.12] blur-[140px] pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] rounded-full bg-purple-600/[0.10] blur-[140px] pointer-events-none"></div>

      <div className="w-full max-w-md bg-[#0a0e1c]/90 border border-white/[0.08] rounded-3xl p-8 sm:p-10 shadow-2xl backdrop-blur-2xl relative z-10 transition-all duration-300 hover:border-white/[0.14]">
        {/* Brand Header */}
        <div className="text-center mb-8">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-purple-500 mx-auto mb-4 flex items-center justify-center shadow-lg shadow-indigo-500/25">
            <span className="text-xl font-black text-white">IF</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white via-zinc-100 to-zinc-400 bg-clip-text text-transparent">
            Welcome to InvoiceFlow
          </h1>
          <p className="text-zinc-400 text-xs sm:text-sm mt-1.5">Sign in to manage client invoices & contracts</p>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-rose-500/10 border border-rose-500/25 rounded-xl flex items-start gap-3 text-rose-300 text-xs">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-zinc-300 uppercase tracking-wider block">Email Address</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 flex items-center pl-3.5 text-zinc-500">
                <Mail className="w-4 h-4" />
              </span>
              <input
                type="email"
                {...register('email')}
                className={`w-full pl-10 pr-4 py-2.5 bg-white/[0.02] hover:bg-white/[0.03] border ${
                  errors.email ? 'border-rose-500/60 focus:border-rose-500' : 'border-white/[0.08] focus:border-indigo-500/80'
                } rounded-xl text-white text-xs sm:text-sm placeholder-zinc-500 focus:outline-none transition-all duration-200`}
                placeholder="you@company.com"
              />
            </div>
            {errors.email && <p className="text-rose-400 text-[11px] mt-1">{errors.email.message}</p>}
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-[11px] font-bold text-zinc-300 uppercase tracking-wider block">Password</label>
            </div>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 flex items-center pl-3.5 text-zinc-500">
                <KeyRound className="w-4 h-4" />
              </span>
              <input
                type={showPassword ? 'text' : 'password'}
                {...register('password')}
                className={`w-full pl-10 pr-12 py-2.5 bg-white/[0.02] hover:bg-white/[0.03] border ${
                  errors.password ? 'border-rose-500/60 focus:border-rose-500' : 'border-white/[0.08] focus:border-indigo-500/80'
                } rounded-xl text-white text-xs sm:text-sm placeholder-zinc-500 focus:outline-none transition-all duration-200`}
                placeholder="••••••••"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute inset-y-0 right-0 flex items-center pr-3.5 text-zinc-500 hover:text-zinc-200 transition-colors"
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            {errors.password && <p className="text-rose-400 text-[11px] mt-1">{errors.password.message}</p>}
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 px-4 bg-gradient-to-r from-indigo-500 via-indigo-600 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white text-xs sm:text-sm font-bold rounded-xl shadow-lg shadow-indigo-500/25 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 disabled:opacity-50 disabled:cursor-not-allowed active:scale-[0.98] transition-all duration-200"
          >
            {loading ? (
              <span className="flex items-center justify-center gap-2">
                <span className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin"></span>
                Authenticating...
              </span>
            ) : (
              'Sign In to Dashboard'
            )}
          </button>
        </form>

        {/* Demo Credentials Quick-Select */}
        <div className="mt-6 p-4 rounded-2xl bg-white/[0.02] border border-white/[0.06]">
          <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider block mb-2">
            Demo Credentials (1-Click Fill)
          </span>
          <div className="flex flex-col gap-2">
            <button
              type="button"
              onClick={() => fillDemo('ytadesse72@gmail.com')}
              className="text-left px-3 py-2 rounded-xl bg-white/[0.02] hover:bg-indigo-500/10 border border-white/[0.05] hover:border-indigo-500/30 transition-all flex items-center justify-between group"
            >
              <div>
                <span className="text-xs font-semibold text-zinc-200 group-hover:text-indigo-300 block">
                  ytadesse72@gmail.com
                </span>
                <span className="text-[10px] text-zinc-500">Full Workspace (24 Invoices & Clients)</span>
              </div>
              <span className="text-[10px] font-mono-num text-zinc-400 group-hover:text-indigo-400 bg-white/[0.03] px-2 py-0.5 rounded border border-white/[0.06]">
                password123
              </span>
            </button>

            <button
              type="button"
              onClick={() => fillDemo('test@invoiceflow.com')}
              className="text-left px-3 py-2 rounded-xl bg-white/[0.02] hover:bg-indigo-500/10 border border-white/[0.05] hover:border-indigo-500/30 transition-all flex items-center justify-between group"
            >
              <div>
                <span className="text-xs font-semibold text-zinc-200 group-hover:text-indigo-300 block">
                  test@invoiceflow.com
                </span>
                <span className="text-[10px] text-zinc-500">Test Accountant Account</span>
              </div>
              <span className="text-[10px] font-mono-num text-zinc-400 group-hover:text-indigo-400 bg-white/[0.03] px-2 py-0.5 rounded border border-white/[0.06]">
                password123
              </span>
            </button>
          </div>
        </div>

        <div className="mt-8 text-center text-xs text-zinc-400">
          Don't have an account yet?{' '}
          <Link to="/register" className="text-indigo-400 hover:text-indigo-300 font-bold transition-colors">
            Create account
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Login;
