const printLink = document.getElementById('printLink');

if(printLink != null && printLink != 'undefined' ) {

    printLink.addEventListener("click", function (e) {
        e.preventDefault();
        window.print();
    });
}