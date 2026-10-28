import type { Metadata, Viewport } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'NagarSetu V2 — Municipal Command Center',
  description: 'Smart City Civic Platform — Real-time SOS, Incident Intelligence & Responder Dispatch for Bhopal & Indore',
  manifest: '/manifest.json',
  icons: { icon: '/favicon.ico' },
  applicationName: 'NagarSetu',
  keywords: ['smart city', 'civic tech', 'emergency response', 'municipal dashboard', 'Bhopal', 'Indore'],
};

export const viewport: Viewport = {
  themeColor: '#070714',
  width: 'device-width',
  initialScale: 1,
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark" suppressHydrationWarning>
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `(function() {
              try {
                var t = localStorage.getItem('nagarsetu_theme');
                if (t === 'light') {
                  document.documentElement.classList.remove('dark');
                } else if (t === 'dark') {
                  document.documentElement.classList.add('dark');
                }
              } catch(e) {}
            })()`
          }}
        />
      </head>
      <body className="bg-void text-ink font-sans antialiased overflow-hidden h-screen w-screen transition-colors duration-200">
        {children}
      </body>
    </html>
  );
}
