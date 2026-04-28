import os
import sys

os.environ.setdefault("GH_TOKEN", "test-dummy-token")

sys.path.insert(0, os.path.dirname(__file__))
from update_metadata import summarize_check_runs  # noqa: I001


def test_single_null_conclusion():
    assert summarize_check_runs({"Jenkins": None}) == "success"

def test_all_null_conclusions():
    assert summarize_check_runs({"Jenkins": None, "CI": None}) == "success"

def test_empty_checks():
    assert summarize_check_runs({}) == "success"

def test_null_alongside_success():
    assert summarize_check_runs({"Jenkins": None, "CI": "success"}) == "success"

def test_null_alongside_failure():
    assert summarize_check_runs({"Jenkins": None, "CI": "failure"}) == "failure"

def test_null_alongside_timed_out():
    assert summarize_check_runs({"Jenkins": None, "CI": "timed_out"}) == "failure"

def test_null_alongside_cancelled():
    assert summarize_check_runs({"Jenkins": None, "CI": "cancelled"}) == "failure"

def test_all_success():
    assert summarize_check_runs({"Jenkins": "success", "CI": "success"}) == "success"

def test_one_failure():
    assert summarize_check_runs({"Jenkins": "failure", "CI": "success"}) == "failure"

def test_timed_out():
    assert summarize_check_runs({"Jenkins": "timed_out"}) == "failure"

def test_cancelled():
    assert summarize_check_runs({"Jenkins": "cancelled"}) == "failure"

def test_pending_conclusion():
    assert summarize_check_runs({"Jenkins": "pending", "CI": "success"}) == "pending"

def test_pending_takes_priority_over_success():
    assert summarize_check_runs({"A": "success", "B": "pending"}) == "pending"

def test_neutral_conclusion():
    assert summarize_check_runs({"Jenkins": "neutral"}) == "neutral"

def test_skipped_conclusion():
    assert summarize_check_runs({"Jenkins": "skipped"}) == "neutral"

def test_mixed_success_and_neutral():
    assert summarize_check_runs({"A": "success", "B": "neutral"}) == "neutral"

def test_failure_takes_priority_over_pending():
    assert summarize_check_runs({"A": "failure", "B": "pending"}) == "failure"