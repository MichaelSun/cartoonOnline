import os
import sys
import re
import optparse
import myLib

CHECK_FILE = ['AndroidManifest.xml', 'res', 'src', 'src_lib']
FILE_SUBFIX = ['.java', '.xml']
MENIFEST_FILE = 'AndroidManifest.xml'

init_optprarse = optparse.OptionParser(usage='python replace.py -p your_new_package_name')
init_optprarse.add_option('-p', '--package', dest='package')

class ARGUMENTS_ERROR(Exception):
    """ replace text failure
    """

def __getPackageName():
    if os.path.exists(MENIFEST_FILE):
        with open(MENIFEST_FILE, 'r') as mfile:
            for line in mfile:
                m = re.search('package=\".*\"', line)
                if m:
                    oldStr = m.group(0)
                    #print oldStr + ' left index = ' + str(oldStr.find('\"')) + ' right index = ' + str(oldStr.rfind('\"'))
                    return oldStr[oldStr.find('\"') + 1:oldStr.rfind('\"')]

    return None

def __walk_replace_file(filename, old, new):
    if filename == None or len(filename) == 0:
        raise ARGUMENTS_ERROR()

    if os.path.isfile(filename):
        if __check_file_extend(filename):
            print 'find one file can replace, file : %s' % filename
            myLib.replce_text_in_file(filename, old, new)
    elif os.path.isdir(filename):
        wpath = os.walk(filename)
        for item in wpath:
            files = item[2]
            parentPath = item[0]
            for f in files:
                if __check_file_extend(f):
                    print 'find one file can replace, file : %s/%s' % (parentPath, f)
                    myLib.replce_text_in_file('%s/%s' % (parentPath, f), old, new)
    
    return True
                
def __check_file_extend(filename):
    for end in FILE_SUBFIX:
        if filename.endswith(end):
            return True
    return False

def __main(args):
    opt, arg = init_optprarse.parse_args(args)
    new_package = opt.package

    if new_package == None or len(new_package) == 0:
        raise ARGUMENTS_ERROR()

    old_package = __getPackageName()

    print '[[replace.py]] try to replace old package : %s to new pacakge : %s' % (old_package, new_package)
    for item in CHECK_FILE:
        __walk_replace_file(item, old_package, new_package)

    return True

if __name__ == '__main__':
    __main(sys.argv[1:])
