int sum(int a, int b) {
    return a+b;
}

int sub(int a, int b) {
    return sum(a, -b);
}
void main() {
    int a = 3;
    int b = 5;
    int c = 0;
    if(0){
        sub(1,2);
    }else{
        sum(1,2);
        while(c<a){
            ++c;
        }
    }
    _print(sum(a,b));
    _print(sub(a,b));
    _print(sub(sum(a,b),2));
}