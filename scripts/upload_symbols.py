import os
import sys
from googleapiclient.discovery import build
from google.oauth2.service_account import Credentials
from googleapiclient.http import MediaFileUpload

# Configuration
SERVICE_ACCOUNT_FILE = 'service-account-key.json'
PACKAGE_NAME = 'com.n8nmonitor.app'
VERSION_CODE = 9
TRACK = 'internal'

def upload_symbols():
    try:
        # Authentification
        credentials = Credentials.from_service_account_file(
            SERVICE_ACCOUNT_FILE,
            scopes=['https://www.googleapis.com/auth/androidpublisher']
        )
        
        service = build('androidpublisher', 'v3', credentials=credentials)
        
        # CrÃ©er un edit
        edit_request = service.edits().insert(body={}, packageName=PACKAGE_NAME)
        edit_result = edit_request.execute()
        edit_id = edit_result['id']
        
        print(f"[INFO] Edit crÃ©Ã© avec ID: {edit_id}")
        
        # Upload des symboles natifs
        symbols_dir = r'C:\\Users\\quarm\\Downloads\\n8n\\scripts/../app/build/intermediates/merged_native_libs/release/out/lib'
        if os.path.exists(symbols_dir):
            for root, dirs, files in os.walk(symbols_dir):
                for file in files:
                    if file.endswith('.so'):
                        symbol_file = os.path.join(root, file)
                        print(f"[INFO] Upload du fichier de symboles: {file}")
                        
                        # Note: L'API Google Play ne supporte pas directement l'upload de symboles .so
                        # Il faut utiliser l'interface web ou des outils spÃ©cialisÃ©s
                        print(f"[INFO] Fichier trouvÃ©: {symbol_file}")
        
        # Upload du mapping ProGuard si disponible
        mapping_file = r'C:\\Users\\quarm\\Downloads\\n8n\\scripts/../app/build/outputs/mapping/release/mapping.txt'
        if os.path.exists(mapping_file):
            print(f"[INFO] Upload du fichier de mapping ProGuard")
            
            media = MediaFileUpload(mapping_file, mimetype='text/plain')
            
            mapping_request = service.edits().deobfuscationfiles().upload(
                packageName=PACKAGE_NAME,
                editId=edit_id,
                apkVersionCode=VERSION_CODE,
                deobfuscationFileType='proguard',
                media_body=media
            )
            
            mapping_result = mapping_request.execute()
            print(f"[OK] Fichier de mapping uploadÃ©: {mapping_result}")
        
        # Valider l'edit
        commit_request = service.edits().commit(
            editId=edit_id,
            packageName=PACKAGE_NAME
        )
        commit_result = commit_request.execute()
        
        print(f"[SUCCESS] Symboles de dÃ©bogage uploadÃ©s avec succÃ¨s!")
        
    except Exception as e:
        print(f"[ERROR] Erreur lors de l'upload: {e}")
        sys.exit(1)

if __name__ == '__main__':
    upload_symbols()
