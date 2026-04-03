import { Navigate, Route, Routes } from 'react-router-dom'
import MainLayout from './layout/MainLayout'
import { MetaScanProvider } from './context/MetaScanContext'
import DashboardPage from './pages/DashboardPage'
import MetadataAnalyzerPage from './pages/MetadataAnalyzerPage'
import ImageForensicsPage from './pages/ImageForensicsPage'
import PrivacyToolsPage from './pages/PrivacyToolsPage'
import OsintToolsPage from './pages/OsintToolsPage'
import ReportsPage from './pages/ReportsPage'
import './App.css'

function App() {
  return (
    <MetaScanProvider>
      <Routes>
        <Route element={<MainLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="analyzer" element={<MetadataAnalyzerPage />} />
          <Route path="image-forensics" element={<ImageForensicsPage />} />
          <Route path="privacy" element={<PrivacyToolsPage />} />
          <Route path="osint" element={<OsintToolsPage />} />
          <Route path="reports" element={<ReportsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </MetaScanProvider>
  )
}

export default App
