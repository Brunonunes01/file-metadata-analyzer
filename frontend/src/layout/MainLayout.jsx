import { NavLink, Outlet } from 'react-router-dom'

const navigationItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/analyzer', label: 'Analyzer' },
  { to: '/image-forensics', label: 'Image Forensics' },
  { to: '/privacy', label: 'Privacy' },
  { to: '/osint', label: 'OSINT' },
  { to: '/reports', label: 'Reports' },
]

function MainLayout() {
  return (
    <div className="platform-shell">
      <aside className="platform-sidebar">
        <div className="sidebar-header">
          <span className="hero-tag">[ SYSTEM ONLINE ]</span>
          <h1>MetaScan</h1>
          <p>OSINT Metadata Platform</p>
        </div>

        <nav className="sidebar-nav" aria-label="Navegação principal">
          {navigationItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <main className="platform-content">
        <Outlet />
      </main>
    </div>
  )
}

export default MainLayout
