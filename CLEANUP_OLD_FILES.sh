#!/bin/bash
# Скрипт для видалення застарілих файлів документації

echo "=== Видалення застарілих файлів ==="

# Застарілі інструкції по встановленню
rm -f webrtc+other.md
rm -f SETUP_INSTRUCTIONS.md
rm -f GROUP_API_INSTALLATION.md
rm -f GROUP_CHAT_DEPLOYMENT.md
rm -f MESSAGES_SCREEN_SIMPLE_PATCH.md
rm -f PHP_SERVER_DEPLOYMENT.md
rm -f CALLS_INTEGRATION.md
rm -f ENCRYPTED_UPLOADS_SETUP.md
rm -f GROUP_CHAT_API.md
rm -f MEDIA_VIEWER_PATCH.md

# Застарілі інструкції в server_modifications
rm -f server_modifications/FIX_PDO_DRIVER.md
rm -f server_modifications/README_INSTALLATION.md
rm -f server_modifications/QUICK_FIX_V3.md
rm -f server_modifications/README_INSTALLATION_V2.md
rm -f server_modifications/INSTALLATION_V2_API.md
rm -f server_modifications/FINAL_FIX_INSTRUCTIONS.md
rm -f server_modifications/README_V2_API.md

# Застарілі інструкції в php_server_files
rm -f php_server_files/README.md

# Застарілі інструкції в nodejs-integration
rm -f nodejs-integration/README_RU.md
rm -f nodejs-integration/REGISTER_CONTROLLERS.md

echo "✅ Застарілі файли видалено!"
echo ""
echo "Залишилися тільки актуальні файли:"
echo "- README.md (основний)"
echo "- GROUPS_FEATURES_ROADMAP.md (roadmap функцій груп)"
echo "- IMPLEMENTATION_DONE.md (що зроблено)"
echo "- IMPLEMENTATION_TODO.md (що треба зробити)"
echo "- IMPLEMENTATION_GUIDE.md (детальна інструкція)"
