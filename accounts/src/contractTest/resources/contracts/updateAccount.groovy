package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should update account"
    request {
        method 'PATCH'
        url '/accounts'
        headers {
            contentType(applicationJson())
            header 'Authorization', 'Bearer test-token'
        }
        body([
            firstName: "Updated",
            lastName: "Name",
            birthDate: "1995-10-20",
            amount: 2000.00
        ])
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            login: "test_user",
            firstName: "Updated",
            lastName: "Name",
            birthDate: "1995-10-20",
            amount: 2000.00
        ])
    }
}
