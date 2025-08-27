import React from 'react'
import ChartView from './chartView'

export type IDevelopProps = {
  params: { appId: string }
}

const Overview = async ({
  params: { appId },
}: IDevelopProps) => {
  return (
    <div className="h-full px-4 sm:px-16 py-6 overflow-scroll">
      {/* <ApikeyInfoPanel /> */}
      <ChartView appId={appId} />
    </div>
  )
}

export default Overview
