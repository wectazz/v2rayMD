import os

translations = {
    'values': {
        'title_app_settings': 'App settings',
        'title_tools_and_info': 'Tools & Info'
    },
    'values-ru': {
        'title_app_settings': 'Настройки приложения',
        'title_tools_and_info': 'Инструменты и информация'
    },
    'values-ar': {
        'title_import_method': 'طريقة الاستيراد',
        'title_add_manual': 'إدخال يدوي',
        'title_actions': 'الإجراءات',
        'title_tests': 'الاختبارات',
        'title_delete': 'حذف',
        'title_app_settings': 'إعدادات التطبيق',
        'title_tools_and_info': 'أدوات ومعلومات'
    },
    'values-bn': {
        'title_import_method': 'আমদানি পদ্ধতি',
        'title_add_manual': 'ম্যানুয়াল এন্ট্রি',
        'title_actions': 'কার্যক্রম',
        'title_tests': 'পরীক্ষা',
        'title_delete': 'মুছুন',
        'title_app_settings': 'অ্যাপ সেটিংস',
        'title_tools_and_info': 'টুলস এবং তথ্য'
    },
    'values-bqi-rIR': {
        'title_import_method': 'روش وارد کردن',
        'title_add_manual': 'ورود دستی',
        'title_actions': 'عملیات',
        'title_tests': 'آزمون‌ها',
        'title_delete': 'حذف',
        'title_app_settings': 'تنظیمات برنامه',
        'title_tools_and_info': 'ابزارها و اطلاعات'
    },
    'values-fa': {
        'title_import_method': 'روش وارد کردن',
        'title_add_manual': 'ورود دستی',
        'title_actions': 'عملیات',
        'title_tests': 'آزمون‌ها',
        'title_delete': 'حذف',
        'title_app_settings': 'تنظیمات برنامه',
        'title_tools_and_info': 'ابزارها و اطلاعات'
    },
    'values-vi': {
        'title_import_method': 'Phương thức nhập',
        'title_add_manual': 'Nhập thủ công',
        'title_actions': 'Hành động',
        'title_tests': 'Kiểm tra',
        'title_delete': 'Xóa',
        'title_app_settings': 'Cài đặt ứng dụng',
        'title_tools_and_info': 'Công cụ & thông tin'
    },
    'values-zh-rCN': {
        'title_import_method': '导入方法',
        'title_add_manual': '手动输入',
        'title_actions': '操作',
        'title_tests': '测试',
        'title_delete': '删除',
        'title_app_settings': '应用设置',
        'title_tools_and_info': '工具和信息'
    },
    'values-zh-rTW': {
        'title_import_method': '導入方法',
        'title_add_manual': '手動輸入',
        'title_actions': '操作',
        'title_tests': '測試',
        'title_delete': '刪除',
        'title_app_settings': '應用設置',
        'title_tools_and_info': '工具和信息'
    }
}

base_path = r'D:\v2rayMD\V2rayMD\app\src\main\res'

for folder, keys in translations.items():
    file_path = os.path.join(base_path, folder, 'strings.xml')
    if os.path.exists(file_path):
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        insert_str = ''
        for k, v in keys.items():
            if f'name=\"{k}\"' not in content:
                insert_str += f'    <string name=\"{k}\">{v}</string>\n'
        
        if insert_str:
            content = content.replace('</resources>', f'{insert_str}</resources>')
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f'Updated {folder}')
