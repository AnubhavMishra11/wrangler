# aggregate-stats

## Description
Aggregates byte sizes and time durations across multiple rows and outputs the results in specified columns with optional unit conversion.

## Syntax
```
aggregate-stats :<byte_size_column> :<time_duration_column> :<total_bytes_column> :<total_time_column> [<byte_size_output_unit>] [<time_output_unit>]
```

## Arguments

| Argument | Description | Default |
| -------- | ----------- | ------- |
| `byte_size_column` | Column containing byte size values (e.g., "10KB", "1.5MB") | Required |
| `time_duration_column` | Column containing time duration values (e.g., "100ms", "1.5s") | Required |
| `total_bytes_column` | Output column for the aggregated byte size | Required |
| `total_time_column` | Output column for the aggregated time duration | Required |
| `byte_size_output_unit` | Output unit for byte size (B, KB, MB, GB, TB, PB) | MB |
| `time_output_unit` | Output unit for time duration (ns, ms, s, m, h, d) | s |

## Usage Notes

- The directive aggregates all byte size values in the `byte_size_column` and all time duration values in the `time_duration_column` across all input rows.
- The aggregated values are output in a single row with the specified output columns.
- The directive handles missing and invalid values by skipping them in the aggregation.
- The directive supports custom output units for both byte sizes and time durations.

## Examples

### Basic Usage

```
aggregate-stats :data_transfer_size :response_time :total_size_mb :total_time_sec
```

This directive aggregates the byte sizes in the `data_transfer_size` column and the time durations in the `response_time` column, and outputs the results in the `total_size_mb` and `total_time_sec` columns using the default units (MB and s).

### Custom Output Units

```
aggregate-stats :data_transfer_size :response_time :total_size_kb :total_time_ms 'KB' 'ms'
```

This directive aggregates the byte sizes and time durations as above, but outputs the results in KB and ms instead of the default MB and s.

## Related Directives

- [set-column](set-column.md): Sets the column value to the result of an expression execution.
- [table-lookup](table-lookup.md): Performs lookups into Table datasets.
