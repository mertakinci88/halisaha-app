/** Tüm sayfaların en altında görünen, yazılımı geliştiren firma bilgisi. */
export default function AltBilgi() {
  return (
    <footer className="border-t border-divider px-5 py-4 text-center text-[12.5px] tracking-[0.02em] text-ink/45">
      © {new Date().getFullYear()} MsC Yazılım
    </footer>
  );
}
