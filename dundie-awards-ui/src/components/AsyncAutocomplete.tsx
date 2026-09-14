import { useState } from 'react'
import Autocomplete from '@mui/material/Autocomplete'
import TextField from '@mui/material/TextField'
import { useQuery } from '@tanstack/react-query'
import { useDebouncedValue } from '../hooks/useDebouncedValue'

type Props<T> = {
  label: string
  queryKey: string
  search: (term: string) => Promise<T[]>
  value: T | null
  onChange: (value: T | null) => void
  getOptionLabel: (option: T) => string
  isOptionEqualToValue: (option: T, value: T) => boolean
  disabled?: boolean
  required?: boolean
  helperText?: string
}

// searches the server as you type, so the picker never depends on loading every row up front
export default function AsyncAutocomplete<T>({
  label,
  queryKey,
  search,
  value,
  onChange,
  getOptionLabel,
  isOptionEqualToValue,
  disabled = false,
  required = false,
  helperText,
}: Props<T>) {
  const [input, setInput] = useState('')
  const term = useDebouncedValue(input.trim())

  const options = useQuery({
    queryKey: [queryKey, term],
    queryFn: () => search(term),
    enabled: !disabled,
  })

  return (
    <Autocomplete
      fullWidth
      disabled={disabled}
      options={options.data ?? []}
      loading={options.isFetching}
      value={value}
      onChange={(_, next) => onChange(next)}
      inputValue={input}
      onInputChange={(_, next, reason) => {
        // keep the typed text when the menu closes without a pick
        if (reason !== 'reset') setInput(next)
      }}
      getOptionLabel={getOptionLabel}
      isOptionEqualToValue={isOptionEqualToValue}
      // the server already filtered; filtering again would hide fresh results
      filterOptions={(option) => option}
      noOptionsText={term ? 'No matches' : 'Start typing to search'}
      renderInput={(params) => (
        <TextField
          {...params}
          label={label}
          required={required}
          helperText={options.error ? 'Could not load options' : helperText}
          error={Boolean(options.error)}
        />
      )}
    />
  )
}
