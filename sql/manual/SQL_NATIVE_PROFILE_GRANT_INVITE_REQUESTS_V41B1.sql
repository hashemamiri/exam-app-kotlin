-- V41B.1: restore authenticated profile RPC permission after deployments with hardening grants missing.
revoke all on function public.native_my_profile() from public, anon;
grant execute on function public.native_my_profile() to authenticated;

revoke all on function public.native_manager_create_teacher_invites_v40b(integer), public.native_manager_invites_v40b(), public.native_manager_revoke_invite_v40b(uuid) from public, anon;
grant execute on function public.native_manager_create_teacher_invites_v40b(integer), public.native_manager_invites_v40b(), public.native_manager_revoke_invite_v40b(uuid) to authenticated;
