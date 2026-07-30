import asyncio
from playwright.async_api import async_playwright

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context()
        page = await context.new_page()
        
        # 1. Login
        print("Navigating to frontend...")
        await page.goto("http://localhost:5173")
        await page.wait_for_selector('input[type="email"]')
        
        print("Logging in...")
        await page.fill('input[type="email"]', "ragtest@example.com")
        await page.fill('input[type="password"]', "Password123!")
        
        # If there's a login button, click it. (Assume standard form submit or button)
        await page.click('button[type="submit"]')
        
        # Wait for navigation to dashboard/documents
        print("Waiting for dashboard...")
        await page.wait_for_url("**/chat*")
        
        # Upload handbook
        print("Uploading document...")
        # Assume there's a file input
        file_input = await page.query_selector('input[type="file"]')
        if file_input:
            await file_input.set_input_files(r"C:\Users\narai\Downloads\ABC_Technologies_Employee_Handbook_2026.docx")
            print("File selected.")
            
            # Click upload if there's a button
            upload_btn = await page.query_selector('button:has-text("Upload")')
            if upload_btn:
                await upload_btn.click()
                print("Upload clicked.")
                await page.wait_for_selector('text=READY', timeout=15000)
                print("Document ready.")
        else:
            print("No file input found directly, maybe need to go to /documents?")
            await page.goto("http://localhost:5173/documents")
            file_input = await page.wait_for_selector('input[type="file"]')
            await file_input.set_input_files(r"C:\Users\narai\Downloads\ABC_Technologies_Employee_Handbook_2026.docx")
            
            upload_btn = await page.query_selector('button:has-text("Upload")')
            if upload_btn:
                await upload_btn.click()
            await page.wait_for_selector('text=READY', timeout=30000)
            print("Document ready.")
            await page.goto("http://localhost:5173/chat")
        
        print("Creating new conversation...")
        # Assuming clicking something to create conversation or it is default
        new_btn = await page.query_selector('button:has-text("New")')
        if new_btn:
            await new_btn.click()
            
        print("Asking question...")
        await page.fill('textarea', "Who is the CEO?")
        await page.click('button:has-text("Send")') # Or whatever icon has send, maybe aria-label
        # Actually aria-label="Send message" is used in ChatPage.tsx
        # Let's use that
        # await page.click('button[aria-label="Send message"]')
        
        print("Waiting for response...")
        await asyncio.sleep(5) # wait for ollama to stream
        
        content = await page.content()
        if "John Smith" in content:
            print("Response contains John Smith")
        else:
            print("Response does NOT contain John Smith")
            
        await browser.close()

if __name__ == "__main__":
    asyncio.run(main())
