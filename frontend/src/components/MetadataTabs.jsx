import { useMemo, useState } from 'react'

function MetadataTabs({ tikaMetadata, exiftoolMetadata, mergedMetadata }) {
  const tabs = useMemo(
    () => [
      { id: 'tika', label: 'Tika Metadata', data: tikaMetadata },
      { id: 'exiftool', label: 'ExifTool Metadata', data: exiftoolMetadata },
      { id: 'merged', label: 'Merged Metadata', data: mergedMetadata },
    ],
    [tikaMetadata, exiftoolMetadata, mergedMetadata],
  )

  const [activeTab, setActiveTab] = useState(tabs[0].id)

  const currentTab = tabs.find((tab) => tab.id === activeTab) || tabs[0]

  return (
    <section className="card">
      <h2>Metadados Técnicos</h2>
      <div className="tabs" role="tablist" aria-label="Seções de metadados técnicos">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            className={`tab-button ${activeTab === tab.id ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <pre className="json-view">{formatJson(currentTab.data)}</pre>
    </section>
  )
}

function formatJson(data) {
  if (!data || (typeof data === 'object' && Object.keys(data).length === 0)) {
    return 'Nenhum metadado disponível nesta seção.'
  }

  try {
    return JSON.stringify(data, null, 2)
  } catch {
    return String(data)
  }
}

export default MetadataTabs
