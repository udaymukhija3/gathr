# Safety + Moderation Checklist

## Onboarding Copy
> "Gathr is a community for real friends. We have zero tolerance for harassment. Be kind, be safe."

## Report Flow
1.  User taps "Report" (on profile or activity).
2.  Selects Reason:
    *   Harassment
    *   Spam
    *   Fake Profile
    *   Dangerous Activity
3.  Adds optional comment.
4.  **Action:**
    *   Hide content immediately for the reporter.
    *   Flag for admin review.
    *   Log event `REPORT_CREATED`.

## Auto-Action Rules
*   **2 Reports (Unique Reporters):** Auto-suspend user for 24h.
*   **5 Reports:** Permanent ban (requires admin review to reverse).

## Data Retention
*   **Chat Logs:** Keep for 30 days for safety audits, then hard delete.
*   **Report Metadata:** Keep forever (for recidivism tracking).
*   **Blocked Users:** Keep forever.
