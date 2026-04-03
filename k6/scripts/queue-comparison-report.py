import json
import sys
from pathlib import Path


def metric_value(metrics, name, key, default=None):
    metric = metrics.get(name, {})
    if key in metric:
        return metric.get(key, default)
    values = metric.get("values", {})
    return values.get(key, default)


def load_summary(path: Path):
    payload = json.loads(path.read_text())
    metrics = payload.get("metrics", {})
    strategy = path.name.replace(".summary.json", "")
    return {
        "strategy": strategy,
        "iterations": metric_value(metrics, "iterations", "count", 0),
        "http_req_failed": metric_value(metrics, "http_req_failed", "value", 0.0),
        "queue_enter_p95": metric_value(metrics, "queue_enter_duration", "p(95)", 0.0),
        "queue_poll_p95": metric_value(metrics, "queue_poll_duration", "p(95)", 0.0),
        "order_p95": metric_value(metrics, "order_with_queue_duration", "p(95)", 0.0),
        "end_to_end_p95": metric_value(metrics, "queue_end_to_end_duration", "p(95)", 0.0),
        "wait_avg": metric_value(metrics, "queue_wait_seconds", "avg", 0.0),
        "wait_p95": metric_value(metrics, "queue_wait_seconds", "p(95)", 0.0),
        "order_success": metric_value(metrics, "queue_order_success", "count", 0),
        "order_fail": metric_value(metrics, "queue_order_fail", "count", 0),
        "token_timeout": metric_value(metrics, "queue_token_timeout", "count", 0),
        "errors": metric_value(metrics, "errors", "value", 0.0),
        "http_req_duration_p95": metric_value(metrics, "http_req_duration", "p(95)", 0.0),
        "http_req_duration_p99": metric_value(metrics, "http_req_duration", "p(99)", 0.0),
    }


def main():
    if len(sys.argv) < 2:
        raise SystemExit("usage: python queue-comparison-report.py <summary.json> [<summary.json> ...]")

    rows = [load_summary(Path(arg)) for arg in sys.argv[1:]]
    rows.sort(key=lambda row: row["strategy"])

    headers = [
        "strategy",
        "order_success",
        "order_fail",
        "token_timeout",
        "errors",
        "http_req_duration_p95",
        "http_req_duration_p99",
        "queue_enter_p95",
        "queue_poll_p95",
        "order_p95",
        "end_to_end_p95",
        "wait_avg",
        "wait_p95",
    ]

    print("| " + " | ".join(headers) + " |")
    print("|" + "|".join(["---"] * len(headers)) + "|")
    for row in rows:
        values = []
        for header in headers:
            value = row[header]
            if isinstance(value, float):
                values.append(f"{value:.2f}")
            else:
                values.append(str(value))
        print("| " + " | ".join(values) + " |")


if __name__ == "__main__":
    main()
