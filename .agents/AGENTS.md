# Enterprise AI End-to-End Automation Rules

When performing end-to-end automation or testing for the Enterprise AI application, strictly follow these rules:

## Credentials
- **Email:** `narainram123456789@gmail.com`
- **Password:** `2005Narain@`
- **Rule:** DO NOT create another user account. Always reuse this existing account because it already contains uploaded documents and previous history.

## Login
1. Open the login page.
2. Login using the credentials above.
3. Wait until the dashboard is completely loaded.
4. If any browser popup appears after login (e.g., "Save password", browser password manager, notifications, update prompts), close or dismiss it before continuing. Do not allow browser popups to block clicks or keyboard input.

## Documents
1. Navigate to the Documents page.
2. Check whether the required document already exists. Required document: `ABC_Technologies_Employee_Handbook_2026.docx` (or the PDF equivalent if present).
3. If the document exists and its status is READY:
   - Do not upload it again.
   - Reuse the existing document.
4. If it does not exist:
   - Upload it from the Downloads folder.
   - Wait until processing finishes.
   - Verify the status becomes READY.
   - Verify extracted text is available.

## Chat
1. Go to AI Chat.
2. Create a NEW conversation unless the test specifically requires previous history.
3. Ask the required questions.
4. Verify answers come from the uploaded documents.

## Validation
If the answer is incorrect, do not guess. Collect evidence and report:
- logged-in user email
- document count
- READY document count
- retrieved chunk count
- prompt length
- endpoint used
- raw response

## Browser Rules
Automatically dismiss:
- Save Password popup
- Password Manager popup
- Browser notification permission popup
- Autofill popup
- Any modal blocking interaction
Do not continue until the UI is clickable.

## Safety
Never delete:
- conversations
- uploaded documents
- account data
unless explicitly instructed.

## Testing Workflow
Login -> Dismiss browser popups -> Verify correct account -> Go to Documents -> Reuse existing document if READY -> Otherwise upload from Downloads -> Wait for READY -> Open AI Chat -> Create NEW conversation -> Run tests -> Report results with evidence.

## Background Tasks
- If you start the backend, frontend, or any background servers during your task, you MUST stop them when you are finished.
