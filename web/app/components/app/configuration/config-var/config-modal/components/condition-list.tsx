import React, { useCallback, useEffect, useState } from 'react'
import Item from './condition-item'

const newItem = {
  varibale: '',
  varType: 'string',
  required: true,
  description: '',
}
const ConditionList = ({
  list,
  onChange,
}) => {
  // if (list.length === 0) return null;
  const [selected, setSelected] = useState(false)
  const [data, setData] = useState(list)

  useEffect(() => {
    setData(list)
  }, [list])

  const handleItemChange = useCallback(
    (index, newItem) => {
      const updatedItems = data.map((item, i) => (i === index ? newItem : item))
      setData(updatedItems)
      onChange(updatedItems)
    },
    [data, onChange],
  )

  const handleItemDelete = (index) => {
    const updatedItems = data.filter((_, i) => i !== index)
    setData(updatedItems)
    onChange(updatedItems)
  }

  return (
    <div className='space-y-2'>
      {data
      && data.map((item, i) => (
        <Item
          payload={item}
          key={i}
          onChange={newItem => handleItemChange(i, newItem)}
          onDelete={() => handleItemDelete(i)}
        />
      ))}
    </div>
  )
}

export default React.memo(ConditionList)
