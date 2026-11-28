-- NQV Khoa Cử Hán Ngữ: chạy toàn bộ SQL trong Supabase → SQL Editor.
create table if not exists public.player_profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null default 'Sĩ tử',
  username text not null,
  progress jsonb not null default '{"xp":0,"completedLessons":[],"passedExams":[],"examAttempts":{},"appointment":"","daysStudied":[],"world":{"x":8,"y":58}}'::jsonb,
  updated_at timestamptz not null default now()
);
alter table public.player_profiles enable row level security;
drop policy if exists "Players can read their own profile" on public.player_profiles;
create policy "Players can read their own profile" on public.player_profiles for select to authenticated using (auth.uid() = id);
drop policy if exists "Players can insert their own profile" on public.player_profiles;
create policy "Players can insert their own profile" on public.player_profiles for insert to authenticated with check (auth.uid() = id);
drop policy if exists "Players can update their own profile" on public.player_profiles;
create policy "Players can update their own profile" on public.player_profiles for update to authenticated using (auth.uid() = id) with check (auth.uid() = id);
